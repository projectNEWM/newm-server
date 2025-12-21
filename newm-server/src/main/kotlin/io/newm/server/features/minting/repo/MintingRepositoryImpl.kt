package io.newm.server.features.minting.repo

import com.google.common.annotations.VisibleForTesting
import com.google.iot.cbor.CborArray
import com.google.iot.cbor.CborInteger
import com.google.iot.cbor.CborMap
import com.google.iot.cbor.CborTextString
import com.google.protobuf.ByteString
import com.google.protobuf.kotlin.toByteString
import io.github.oshai.kotlinlogging.KotlinLogging
import io.newm.chain.grpc.PlutusData
import io.newm.chain.grpc.RedeemerTag
import io.newm.chain.grpc.Signature
import io.newm.chain.grpc.TransactionBuilderResponse
import io.newm.chain.grpc.Utxo
import io.newm.chain.grpc.nativeAsset
import io.newm.chain.grpc.outputUtxo
import io.newm.chain.grpc.plutusData
import io.newm.chain.grpc.plutusDataList
import io.newm.chain.grpc.plutusDataMap
import io.newm.chain.grpc.plutusDataMapItem
import io.newm.chain.grpc.redeemer
import io.newm.chain.util.Blake2b
import io.newm.chain.util.Sha3
import io.newm.chain.util.hexToByteArray
import io.newm.server.config.repo.ConfigRepository
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MINT_CASH_REGISTER_COLLECTION_AMOUNT
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MINT_CASH_REGISTER_MIN_AMOUNT
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MINT_CIP68_POLICY
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MINT_CIP68_SCRIPT_ADDRESS
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MINT_CIP68_UTXO_REFERENCE
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MINT_LEGACY_POLICY_IDS
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MINT_SCRIPT_UTXO_REFERENCE
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MINT_STARTER_TOKEN_UTXO_REFERENCE
import io.newm.server.features.cardano.model.Key
import io.newm.server.features.cardano.repo.CardanoRepository
import io.newm.server.features.collaboration.model.Collaboration
import io.newm.server.features.collaboration.repo.CollaborationRepository
import io.newm.server.features.minting.database.MintingStatusHistoryTable
import io.newm.server.features.minting.database.MintingStatusTransactionEntity
import io.newm.server.features.minting.model.MintInfo
import io.newm.server.features.minting.model.MintingStatusTransactionModel
import io.newm.server.features.song.model.PaymentType
import io.newm.server.features.song.model.Release
import io.newm.server.features.song.model.Song
import io.newm.server.features.song.repo.SongRepository
import io.newm.server.features.user.database.UserEntity
import io.newm.server.features.user.model.User
import io.newm.server.features.user.repo.UserRepository
import io.newm.server.ktx.toReferenceUtxo
import io.newm.server.typealiases.SongId
import io.newm.shared.koin.inject
import io.newm.shared.ktx.coLazy
import io.newm.shared.ktx.containsIgnoreCase
import io.newm.shared.ktx.info
import io.newm.shared.ktx.orZero
import io.newm.shared.ktx.toHexString
import io.newm.txbuilder.ktx.sortByHashAndIx
import io.newm.txbuilder.ktx.toCborObject
import io.newm.txbuilder.ktx.toPlutusData
import java.math.BigDecimal
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.parameter.parametersOf
import org.slf4j.Logger

class MintingRepositoryImpl(
    private val userRepository: UserRepository,
    private val collabRepository: CollaborationRepository,
    private val cardanoRepository: CardanoRepository,
    private val configRepository: ConfigRepository,
) : MintingRepository {
    private val log: Logger by inject { parametersOf(javaClass.simpleName) }
    private val songRepository: SongRepository by inject()
    private val legacyPolicyIds: List<String> by coLazy {
        configRepository.getStrings(CONFIG_KEY_MINT_LEGACY_POLICY_IDS)
    }
    private val logger = KotlinLogging.logger {}

    override suspend fun mint(song: Song): MintInfo {
        return cardanoRepository.withLock {
            val user = userRepository.get(song.ownerId!!)
            val release = songRepository.getRelease(song.releaseId!!)
            val collabs = collabRepository.getAllBySongId(song.id!!)
            val cip68Metadata = buildStreamTokenMetadata(release, song, user, collabs)
            val streamTokensTotal = 100_000_000L
            var streamTokensRemaining = 100_000_000L
            val splitCollabs =
                collabs.filter { it.royaltyRate.orZero() > BigDecimal.ZERO }.sortedByDescending { it.royaltyRate }
            log.info { "splitCollabs for songId: ${song.id} - $splitCollabs" }
            val royaltySum = splitCollabs.sumOf { it.royaltyRate!! }
            require(royaltySum.compareTo(100.toBigDecimal()) == 0) { "Collaboration royalty rates must sum to 100 but was $royaltySum" }
            val streamTokenSplits =
                splitCollabs.mapIndexed { index, collaboration ->
                    val collabUser = userRepository.findByEmail(collaboration.email!!)
                    val splitMultiplier = collaboration.royaltyRate!!.toDouble() / 100.0
                    val amount =
                        if (index < splitCollabs.lastIndex) {
                            // round down to nearest whole token
                            (streamTokensTotal * splitMultiplier).toLong()
                        } else {
                            streamTokensRemaining
                        }
                    streamTokensRemaining -= amount
                    Pair(collabUser.walletAddress!!, amount)
                }
            log.info { "Royalty splits for songId: ${song.id} - $streamTokenSplits" }

            // Convert release.mintPaymentType String to PaymentType enum
            val paymentTypeEnum = PaymentType.valueOf(release.mintPaymentType ?: PaymentType.ADA.name)
            val mintCost = release.mintCost // This will be NEWM amount if NEWM, or ADA amount if ADA

            val cip68ScriptAddress = configRepository.getString(CONFIG_KEY_MINT_CIP68_SCRIPT_ADDRESS)
            val cip68Policy = configRepository.getString(CONFIG_KEY_MINT_CIP68_POLICY)
            val cashRegisterCollectionAmount = configRepository.getLong(CONFIG_KEY_MINT_CASH_REGISTER_COLLECTION_AMOUNT)
            val cashRegisterMinAmount = configRepository.getLong(CONFIG_KEY_MINT_CASH_REGISTER_MIN_AMOUNT)

            val isMainnet = cardanoRepository.isMainnet()
            val newmPolicyId =
                if (isMainnet) CardanoRepository.NEWM_TOKEN_POLICY else CardanoRepository.NEWM_TOKEN_POLICY_PREPROD
            val newmTokenName =
                if (isMainnet) CardanoRepository.NEWM_TOKEN_NAME else CardanoRepository.NEWM_TOKEN_NAME_PREPROD

            var paymentKey: Key? = null
            val paymentUtxo = when (paymentTypeEnum) {
                PaymentType.NEWM -> {
                    paymentKey = cardanoRepository.getKey(song.paymentKeyId!!)
                    requireNotNull(
                        cardanoRepository.queryLiveUtxos(paymentKey.address).firstOrNull { utxo ->
                            utxo.nativeAssetsCount == 1 &&
                                utxo.nativeAssetsList.any { nativeAsset ->
                                    nativeAsset.policy == newmPolicyId &&
                                        nativeAsset.name == newmTokenName &&
                                        nativeAsset.amount == mintCost.toString() // mintCost is release.mintCost, holds NEWM amount
                                }
                        }
                    ) { "NEWM payment UTXO not found or invalid for songId: ${song.id}, looking for $mintCost $newmPolicyId.$newmTokenName" }
                }

                PaymentType.PAYPAL -> {
                    // we'll fund the minting with cash register funds
                    null
                }

                else -> {
                    // Handles PaymentType.ADA and legacy cases (where release.mintPaymentType might be null)
                    paymentKey = cardanoRepository.getKey(song.paymentKeyId!!)
                    val costToUse = if (release.mintPaymentType != null && paymentTypeEnum == PaymentType.ADA) {
                        mintCost // This is release.mintCost, which would be ADA amount
                    } else {
                        @Suppress("DEPRECATION")
                        song.mintCostLovelace // Fallback to legacy song.mintCostLovelace
                    }
                    requireNotNull(
                        cardanoRepository.queryLiveUtxos(paymentKey.address).firstOrNull { utxo ->
                            utxo.lovelace == costToUse.toString() && utxo.nativeAssetsCount == 0
                        }
                    ) {
                        "ADA payment UTXO not found or invalid for songId: ${song.id}, looking for $costToUse lovelace at: ${paymentKey.address}"
                    }
                }
            }

            val cashRegisterKey =
                requireNotNull(cardanoRepository.getKeyByName("cashRegister")) { "cashRegister key not defined!" }

            val cashRegisterUtxos = when (paymentTypeEnum) {
                PaymentType.NEWM -> {
                    val allCashRegisterUtxos = cardanoRepository.queryLiveUtxos(cashRegisterKey.address)
                    val topLovelaceUtxos = allCashRegisterUtxos
                        .filter { it.nativeAssetsCount == 0 }
                        .sortedByDescending { it.lovelace.toLong() }
                        .take(5)
                    val topNewmUtxos = allCashRegisterUtxos
                        .filter {
                            it.nativeAssetsCount == 1 &&
                                it.nativeAssetsList.any { asset ->
                                    asset.policy == newmPolicyId &&
                                        asset.name == newmTokenName
                                }
                        }.sortedByDescending {
                            it.nativeAssetsList
                                .first()
                                .amount
                                .toLong()
                        }.take(5)
                    topLovelaceUtxos + topNewmUtxos
                }

                else -> { // ADA or PayPal
                    cardanoRepository
                        .queryLiveUtxos(cashRegisterKey.address)
                        .filter { it.nativeAssetsCount == 0 }
                        .sortedByDescending { it.lovelace.toLong() }
                        .take(5)
                }
            }

            require(cashRegisterUtxos.isNotEmpty()) { "cashRegister has no utxos!" }
            val cashRegisterAdaAmount = cashRegisterUtxos.sumOf { it.lovelace.toLong() }

            val moneyBoxKey =
                if (cashRegisterAdaAmount >= cashRegisterCollectionAmount + cashRegisterMinAmount) {
                    // we should collect to the moneybox
                    cardanoRepository.getKeyByName("moneyBox")
                } else {
                    null
                }

            val allMoneyBoxUtxos = moneyBoxKey?.let {
                cardanoRepository.queryLiveUtxos(moneyBoxKey.address)
            } ?: emptyList()

            val moneyBoxAdaUtxos =
                moneyBoxKey?.let {
                    allMoneyBoxUtxos
                        .filter { it.nativeAssetsCount == 0 }
                        .sortedByDescending { it.lovelace.toLong() }
                        .take(5)
                }

            val moneyBoxNewmUtxos =
                moneyBoxKey?.let {
                    allMoneyBoxUtxos
                        .filter {
                            it.nativeAssetsCount == 1 &&
                                it.nativeAssetsList.any { asset ->
                                    asset.policy == CardanoRepository.NEWM_TOKEN_POLICY &&
                                        asset.name == CardanoRepository.NEWM_TOKEN_NAME
                                }
                        }.sortedByDescending {
                            it.nativeAssetsList
                                .first()
                                .amount
                                .toLong()
                        }.take(5)
                }

            // sort utxos lexicographically smallest to largest to find the one we'll use as the reference utxo
            val refUtxo =
                (
                    cashRegisterUtxos + listOfNotNull(paymentUtxo) + moneyBoxAdaUtxos.orEmpty() + moneyBoxNewmUtxos.orEmpty()
                ).sortByHashAndIx()
                    .first()

            val (refTokenName, fracTokenName) =
                calculateTokenNames(refUtxo)

            val collateralKey =
                requireNotNull(cardanoRepository.getKeyByName("collateral")) { "collateral key not defined!" }

            val collateralUtxo =
                requireNotNull(
                    cardanoRepository
                        .queryLiveUtxos(collateralKey.address)
                        .filter { it.nativeAssetsCount == 0 }
                        .maxByOrNull { it.lovelace.toLong() }
                ) { "collateral utxo not found!" }

            val starterTokenUtxoReference =
                configRepository.getString(CONFIG_KEY_MINT_STARTER_TOKEN_UTXO_REFERENCE).toReferenceUtxo()

            val scriptUtxoReference =
                configRepository.getString(CONFIG_KEY_MINT_SCRIPT_UTXO_REFERENCE).toReferenceUtxo()

            val signingKeys = listOfNotNull(cashRegisterKey, moneyBoxKey, paymentKey, collateralKey)

            var transactionBuilderResponse =
                buildMintingTransaction(
                    paymentUtxo = paymentUtxo,
                    cashRegisterUtxos = cashRegisterUtxos,
                    changeAddress = cashRegisterKey.address,
                    moneyBoxUtxos = moneyBoxAdaUtxos,
                    moneyBoxAddress = moneyBoxKey?.address,
                    cashRegisterCollectionAmount = cashRegisterCollectionAmount,
                    collateralUtxo = collateralUtxo,
                    collateralReturnAddress = collateralKey.address,
                    cip68ScriptAddress = cip68ScriptAddress,
                    cip68Metadata = cip68Metadata,
                    cip68Policy = cip68Policy,
                    refTokenName = refTokenName,
                    fracTokenName = fracTokenName,
                    newmPolicyId = newmPolicyId,
                    newmTokenName = newmTokenName,
                    streamTokenSplits = streamTokenSplits,
                    requiredSigners = signingKeys,
                    starterTokenUtxoReference = starterTokenUtxoReference,
                    mintScriptUtxoReference = scriptUtxoReference,
                    signatures = cardanoRepository.signTransactionDummy(signingKeys)
                )

            // check for errors in tx building
            if (transactionBuilderResponse.hasErrorMessage()) {
                throw IllegalStateException("TransactionBuilder Error!: ${transactionBuilderResponse.errorMessage}")
            }

            val transactionIdBytes = transactionBuilderResponse.transactionId.hexToByteArray()

            // get signatures for this transaction
            transactionBuilderResponse =
                buildMintingTransaction(
                    paymentUtxo = paymentUtxo,
                    cashRegisterUtxos = cashRegisterUtxos,
                    changeAddress = cashRegisterKey.address,
                    moneyBoxUtxos = moneyBoxAdaUtxos,
                    moneyBoxAddress = moneyBoxKey?.address,
                    cashRegisterCollectionAmount = cashRegisterCollectionAmount,
                    collateralUtxo = collateralUtxo,
                    collateralReturnAddress = collateralKey.address,
                    cip68ScriptAddress = cip68ScriptAddress,
                    cip68Metadata = cip68Metadata,
                    cip68Policy = cip68Policy,
                    refTokenName = refTokenName,
                    fracTokenName = fracTokenName,
                    newmPolicyId = newmPolicyId,
                    newmTokenName = newmTokenName,
                    streamTokenSplits = streamTokenSplits,
                    requiredSigners = signingKeys,
                    starterTokenUtxoReference = starterTokenUtxoReference,
                    mintScriptUtxoReference = scriptUtxoReference,
                    signatures = cardanoRepository.signTransaction(transactionIdBytes, signingKeys),
                )

            // check for errors in tx building
            if (transactionBuilderResponse.hasErrorMessage()) {
                throw IllegalStateException("TransactionBuilder Error!: ${transactionBuilderResponse.errorMessage}")
            }

            val submitTransactionResponse =
                cardanoRepository.submitTransaction(transactionBuilderResponse.transactionCbor)

            return@withLock if (submitTransactionResponse.result == "MsgAcceptTx") {
                MintInfo(
                    transactionId = submitTransactionResponse.txId,
                    policyId = cip68Policy,
                    assetName = fracTokenName,
                )
            } else {
                throw IllegalStateException(submitTransactionResponse.result)
            }
        }
    }

    override suspend fun updateTokenMetadata(song: Song): MintInfo {
        return cardanoRepository.withLock {
            val user = userRepository.get(song.ownerId!!)
            val release = songRepository.getRelease(song.releaseId!!)
            val collabs = collabRepository.getAllBySongId(song.id!!)
            val cip68Metadata = buildStreamTokenMetadata(release, song, user, collabs)

            val cip68ScriptAddress = configRepository.getString(CONFIG_KEY_MINT_CIP68_SCRIPT_ADDRESS)
            val cip68Policy = configRepository.getString(CONFIG_KEY_MINT_CIP68_POLICY)

            val cashRegisterKey =
                requireNotNull(cardanoRepository.getKeyByName("cashRegister")) { "cashRegister key not defined!" }

            val cashRegisterUtxos = cardanoRepository
                .queryLiveUtxos(cashRegisterKey.address)
                .filter { it.nativeAssetsCount == 0 }
                .sortedByDescending { it.lovelace.toLong() }
                .take(5)

            require(cashRegisterUtxos.isNotEmpty()) { "cashRegister has no utxos!" }

            val refTokenName = song.nftName!!.replaceFirst(PREFIX_FRAC_TOKEN, PREFIX_REF_TOKEN)
            val refTokenUtxo = cardanoRepository.queryUtxoByNativeAsset(cip68Policy, refTokenName)

            val collateralKey =
                requireNotNull(cardanoRepository.getKeyByName("collateral")) { "collateral key not defined!" }

            val collateralUtxo =
                requireNotNull(
                    cardanoRepository
                        .queryLiveUtxos(collateralKey.address)
                        .filter { it.nativeAssetsCount == 0 }
                        .maxByOrNull { it.lovelace.toLong() }
                ) { "collateral utxo not found!" }

            val starterTokenUtxoReference =
                configRepository.getString(CONFIG_KEY_MINT_STARTER_TOKEN_UTXO_REFERENCE).toReferenceUtxo()

            val cip68ScriptUtxoReference =
                configRepository.getString(CONFIG_KEY_MINT_CIP68_UTXO_REFERENCE).toReferenceUtxo()

            val signingKeys = listOfNotNull(cashRegisterKey, collateralKey)

            var transactionBuilderResponse =
                buildCip68UpdateTransaction(
                    refTokenUtxo = refTokenUtxo,
                    cashRegisterUtxos = cashRegisterUtxos,
                    changeAddress = cashRegisterKey.address,
                    collateralUtxo = collateralUtxo,
                    collateralReturnAddress = collateralKey.address,
                    cip68ScriptAddress = cip68ScriptAddress,
                    cip68Metadata = cip68Metadata,
                    cip68Policy = cip68Policy,
                    refTokenName = refTokenName,
                    requiredSigners = signingKeys,
                    starterTokenUtxoReference = starterTokenUtxoReference,
                    cip68ScriptUtxoReference = cip68ScriptUtxoReference,
                    signatures = cardanoRepository.signTransactionDummy(signingKeys)
                )

            // check for errors in tx building
            if (transactionBuilderResponse.hasErrorMessage()) {
                throw IllegalStateException("TransactionBuilder Error!: ${transactionBuilderResponse.errorMessage}")
            }

            val transactionIdBytes = transactionBuilderResponse.transactionId.hexToByteArray()

            // get signatures for this transaction
            transactionBuilderResponse =
                buildCip68UpdateTransaction(
                    refTokenUtxo = refTokenUtxo,
                    cashRegisterUtxos = cashRegisterUtxos,
                    changeAddress = cashRegisterKey.address,
                    collateralUtxo = collateralUtxo,
                    collateralReturnAddress = collateralKey.address,
                    cip68ScriptAddress = cip68ScriptAddress,
                    cip68Metadata = cip68Metadata,
                    cip68Policy = cip68Policy,
                    refTokenName = refTokenName,
                    requiredSigners = signingKeys,
                    starterTokenUtxoReference = starterTokenUtxoReference,
                    cip68ScriptUtxoReference = cip68ScriptUtxoReference,
                    signatures = cardanoRepository.signTransaction(transactionIdBytes, signingKeys),
                )

            // check for errors in tx building
            if (transactionBuilderResponse.hasErrorMessage()) {
                throw IllegalStateException("TransactionBuilder Error!: ${transactionBuilderResponse.errorMessage}")
            }

            val submitTransactionResponse =
                cardanoRepository.submitTransaction(transactionBuilderResponse.transactionCbor)

            return@withLock if (submitTransactionResponse.result == "MsgAcceptTx") {
                MintInfo(
                    transactionId = submitTransactionResponse.txId,
                    policyId = cip68Policy,
                    assetName = refTokenName,
                )
            } else {
                throw IllegalStateException(submitTransactionResponse.result)
            }
        }
    }

    override fun getTokenAgreementFileIndex(policyId: String): Int = if (policyId in legacyPolicyIds) 1 else 0

    override fun getAudioClipFileIndex(policyId: String): Int = if (policyId in legacyPolicyIds) 0 else 1

    @VisibleForTesting
    internal suspend fun buildMintingTransaction(
        paymentUtxo: Utxo?,
        cashRegisterUtxos: List<Utxo>,
        changeAddress: String,
        moneyBoxUtxos: List<Utxo>?,
        moneyBoxAddress: String?,
        cashRegisterCollectionAmount: Long,
        collateralUtxo: Utxo,
        collateralReturnAddress: String,
        cip68ScriptAddress: String,
        cip68Metadata: PlutusData,
        cip68Policy: String,
        refTokenName: String,
        fracTokenName: String,
        newmPolicyId: String,
        newmTokenName: String,
        streamTokenSplits: List<Pair<String, Long>>,
        requiredSigners: List<Key>,
        starterTokenUtxoReference: Utxo,
        mintScriptUtxoReference: Utxo,
        signatures: List<Signature> = emptyList()
    ) = cardanoRepository.buildTransaction {
        with(sourceUtxos) {
            paymentUtxo?.let { add(it) }
            moneyBoxUtxos?.let { addAll(it) }
            addAll(cashRegisterUtxos)
        }
        with(outputUtxos) {
            // reference NFT output to cip68 script address
            add(
                outputUtxo {
                    address = cip68ScriptAddress
                    // lovelace = "0" auto-calculated minutxo
                    nativeAssets.add(
                        nativeAsset {
                            policy = cip68Policy
                            name = refTokenName
                            amount = "1"
                        }
                    )
                    datumHash = Blake2b.hash256(cip68Metadata.toCborObject().toCborByteArray()).toHexString()
                }
            )

            // fraction RFT output to each artist's wallet
            streamTokenSplits.forEach { (artistWalletAddress, streamTokenAmount) ->
                add(
                    outputUtxo {
                        address = artistWalletAddress
                        // lovelace = "0" auto-calculated minutxo
                        nativeAssets.add(
                            nativeAsset {
                                policy = cip68Policy
                                name = fracTokenName
                                amount = streamTokenAmount.toString()
                            }
                        )
                    }
                )
            }

            val moneyBoxNewmUtxos = moneyBoxUtxos
                ?.filter {
                    it.nativeAssetsCount == 1 &&
                        it.nativeAssetsList.any { asset ->
                            asset.policy == newmPolicyId &&
                                asset.name == newmTokenName
                        }
                }.orEmpty()
            val cashRegisterNewmUtxos = cashRegisterUtxos.filter {
                it.nativeAssetsCount == 1 &&
                    it.nativeAssetsList.any { asset ->
                        asset.policy == newmPolicyId &&
                            asset.name == newmTokenName
                    }
            }
            val paymentNewmUtxos = listOfNotNull(paymentUtxo).filter {
                it.nativeAssetsCount == 1 &&
                    it.nativeAssetsList.any { asset ->
                        asset.policy == newmPolicyId &&
                            asset.name == newmTokenName
                    }
            }

            // collect some into the moneyBox
            moneyBoxAddress?.let { moneyBoxAddress ->
                add(
                    // ada-only output to moneyBox address
                    outputUtxo {
                        address = moneyBoxAddress
                        lovelace =
                            (
                                cashRegisterCollectionAmount + (
                                    moneyBoxUtxos
                                        ?.filter { it.nativeAssetsCount == 0 }
                                        ?.sumOf { it.lovelace.toLong() }
                                        .orZero()
                                )
                            ).toString()
                    }
                )

                (moneyBoxNewmUtxos + cashRegisterNewmUtxos + paymentNewmUtxos).takeIf { it.isNotEmpty() }?.let {
                    add(
                        outputUtxo {
                            address = moneyBoxAddress
                            nativeAssets.add(
                                nativeAsset {
                                    policy = newmPolicyId
                                    name = newmTokenName
                                    amount =
                                        it
                                            .sumOf { utxo ->
                                                utxo.nativeAssetsList
                                                    .firstOrNull { nativeAsset ->
                                                        nativeAsset.policy == newmPolicyId &&
                                                            nativeAsset.name == newmTokenName
                                                    }?.amount
                                                    ?.toLongOrNull() ?: 0L
                                            }.toString()
                                }
                            )
                        }
                    )
                }
            } ?: run {
                // if not collecting to moneyBox, roll up any newm tokens to the cash register
                (cashRegisterNewmUtxos + paymentNewmUtxos).takeIf { it.isNotEmpty() }?.let {
                    add(
                        outputUtxo {
                            address = changeAddress
                            nativeAssets.add(
                                nativeAsset {
                                    policy = newmPolicyId
                                    name = newmTokenName
                                    amount =
                                        it
                                            .sumOf { utxo ->
                                                utxo.nativeAssetsList
                                                    .firstOrNull { nativeAsset ->
                                                        nativeAsset.policy == newmPolicyId &&
                                                            nativeAsset.name == newmTokenName
                                                    }?.amount
                                                    ?.toLongOrNull() ?: 0L
                                            }.toString()
                                }
                            )
                        }
                    )
                }
            }
        }

        this.changeAddress = changeAddress

        with(mintTokens) {
            add(
                nativeAsset {
                    policy = cip68Policy
                    name = refTokenName
                    amount = "1"
                }
            )
            add(
                nativeAsset {
                    policy = cip68Policy
                    name = fracTokenName
                    amount = "100000000"
                }
            )
        }

        collateralUtxos.add(collateralUtxo)
        this.collateralReturnAddress = collateralReturnAddress

        this.requiredSigners.addAll(requiredSigners.map { key -> key.requiredSigner().toByteString() })

        with(referenceInputs) {
            add(starterTokenUtxoReference)
            add(mintScriptUtxoReference)
        }

        if (signatures.isNotEmpty()) {
            this.signatures.addAll(signatures)
        }

        redeemers.add(
            redeemer {
                tag = RedeemerTag.MINT
                index = 0L
                data =
                    plutusData {
                        constr = 0
                        list = plutusDataList { }
                    }
                // calculated
                // exUnits = exUnits {
                //    mem = 2895956L
                //    steps = 793695629L
                // }
            }
        )

        with(datums) {
            add(cip68Metadata)
        }

        // Add a simple transaction metadata note for NEWM Mint - See: CIP-20
        transactionMetadataCbor =
            ByteString.copyFrom(
                CborMap
                    .create(
                        mapOf(
                            CborInteger.create(674) to
                                CborMap.create(
                                    mapOf(
                                        CborTextString.create("msg") to
                                            CborArray.create().apply {
                                                add(CborTextString.create("NEWM Mint"))
                                            }
                                    )
                                )
                        )
                    ).toCborByteArray()
            )
    }

    @VisibleForTesting
    internal suspend fun buildCip68UpdateTransaction(
        refTokenUtxo: Utxo,
        cashRegisterUtxos: List<Utxo>,
        changeAddress: String,
        collateralUtxo: Utxo,
        collateralReturnAddress: String,
        cip68ScriptAddress: String,
        cip68Metadata: PlutusData,
        cip68Policy: String,
        refTokenName: String,
        requiredSigners: List<Key>,
        starterTokenUtxoReference: Utxo,
        cip68ScriptUtxoReference: Utxo,
        signatures: List<Signature> = emptyList(),
    ): TransactionBuilderResponse {
        val redeemerSpendIndex =
            (listOf(refTokenUtxo) + cashRegisterUtxos).sortByHashAndIx().indexOf(refTokenUtxo).toLong()

        return cardanoRepository.buildTransaction {
            with(sourceUtxos) {
                add(refTokenUtxo)
                addAll(cashRegisterUtxos)
            }
            with(outputUtxos) {
                // reference NFT output to cip68 script address
                add(
                    outputUtxo {
                        address = cip68ScriptAddress
                        // lovelace = "0" auto-calculated minutxo
                        nativeAssets.add(
                            nativeAsset {
                                policy = cip68Policy
                                name = refTokenName
                                amount = "1"
                            }
                        )
                        datumHash = Blake2b.hash256(cip68Metadata.toCborObject().toCborByteArray()).toHexString()
                    }
                )
            }

            this.changeAddress = changeAddress

            collateralUtxos.add(collateralUtxo)
            this.collateralReturnAddress = collateralReturnAddress

            this.requiredSigners.addAll(requiredSigners.map { key -> key.requiredSigner().toByteString() })

            with(referenceInputs) {
                add(starterTokenUtxoReference)
                add(cip68ScriptUtxoReference)
            }

            if (signatures.isNotEmpty()) {
                this.signatures.addAll(signatures)
            }

            // {
            //  "constructor": 1,
            //  "fields": [
            //    {
            //      "constructor": 0,
            //      "fields": [
            //        {
            //          "int": 0  # amount to increase or decrease minutxo by
            //        }
            //      ]
            //    },
            //    {
            //      "int": 0  # zero means increase minutxo, 1 means decrease minutxo
            //    }
            //  ]
            // }
            redeemers.add(
                redeemer {
                    tag = RedeemerTag.SPEND
                    index = redeemerSpendIndex
                    data =
                        plutusData {
                            constr = 1
                            list = plutusDataList { }
                        }
                    // calculated
                    // exUnits = exUnits {
                    //    mem = 2895956L
                    //    steps = 793695629L
                    // }
                }
            )

            with(datums) {
                // must provide the datum for the reference token being spent
                add(refTokenUtxo.datum)
                // and the new datum for the re-minted reference token
                add(cip68Metadata)
            }

            // Add a simple transaction metadata note for NEWM Mint - See: CIP-20
            transactionMetadataCbor =
                ByteString.copyFrom(
                    CborMap
                        .create(
                            mapOf(
                                CborInteger.create(674) to
                                    CborMap.create(
                                        mapOf(
                                            CborTextString.create("msg") to
                                                CborArray.create().apply {
                                                    add(CborTextString.create("NEWM Update"))
                                                }
                                        )
                                    )
                            )
                        ).toCborByteArray()
                )
        }
    }

    /**
     * Return the token name for the reference token (100) and the fractional tokens (444)
     */
    @VisibleForTesting
    internal fun calculateTokenNames(utxo: Utxo): Pair<String, String> {
        val txHash = Sha3.hash256(utxo.hash.hexToByteArray())
        val txHashHex = (ByteArray(1) { utxo.ix.toByte() } + txHash).copyOfRange(0, 28).toHexString()
        return Pair(
            PREFIX_REF_TOKEN + txHashHex,
            PREFIX_FRAC_TOKEN + txHashHex,
        )
    }

    @VisibleForTesting
    internal fun buildStreamTokenMetadata(
        release: Release,
        song: Song,
        user: User,
        collabs: List<Collaboration>
    ): PlutusData =
        plutusData {
            constr = 0
            list =
                plutusDataList {
                    with(listItem) {
                        add(
                            plutusData {
                                map =
                                    plutusDataMap {
                                        with(mapItem) {
                                            add(
                                                plutusDataMapItem {
                                                    mapItemKey = "name".toPlutusData()
                                                    mapItemValue = song.title!!.toPlutusData()
                                                }
                                            )
                                            add(
                                                plutusDataMapItem {
                                                    mapItemKey = "image".toPlutusData()
                                                    mapItemValue = release.arweaveCoverArtUrl!!.toPlutusData()
                                                }
                                            )
                                            add(
                                                plutusDataMapItem {
                                                    mapItemKey = "mediaType".toPlutusData()
                                                    mapItemValue = "image/webp".toPlutusData()
                                                }
                                            )
                                            add(
                                                plutusDataMapItem {
                                                    mapItemKey = "music_metadata_version".toPlutusData()
                                                    mapItemValue = plutusData { int = 3 } // CIP-60 Version 3
                                                }
                                            )
                                            add(createPlutusDataRelease(release, collabs))
                                            add(createPlutusDataFiles(song, user, collabs))
                                            // DO NOT ADD LINKS for now. Revisit later
                                            // add(createPlutusDataLinks(user, collabs))
                                        }
                                    }
                            }
                        )
                        // CIP-68 Version
                        add(plutusData { int = 1L })
                    }
                }
        }

    private fun createPlutusDataRelease(
        release: Release,
        collabs: List<Collaboration>
    ) = plutusDataMapItem {
        mapItemKey = "release".toPlutusData()
        mapItemValue =
            plutusData {
                map =
                    plutusDataMap {
                        with(mapItem) {
                            add(
                                plutusDataMapItem {
                                    mapItemKey = "release_type".toPlutusData()
                                    mapItemValue =
                                        "Single".toPlutusData() // All Singles for now
                                }
                            )
                            add(
                                plutusDataMapItem {
                                    mapItemKey = "release_title".toPlutusData()
                                    mapItemValue = release.title!!.toPlutusData()
                                }
                            )
                            add(
                                plutusDataMapItem {
                                    mapItemKey = "release_date".toPlutusData()
                                    mapItemValue = release.releaseDate!!.toString().toPlutusData()
                                }
                            )
                            release.publicationDate?.let {
                                add(
                                    plutusDataMapItem {
                                        mapItemKey = "publication_date".toPlutusData()
                                        mapItemValue = it.toString().toPlutusData()
                                    }
                                )
                            }
                            add(
                                plutusDataMapItem {
                                    mapItemKey = "distributor".toPlutusData()
                                    mapItemValue = "https://newm.io".toPlutusData()
                                }
                            )
                            val visualArtists =
                                collabs.filter { it.roles?.containsIgnoreCase("Artwork") == true && it.credited == true }
                            if (visualArtists.isNotEmpty()) {
                                transaction {
                                    add(
                                        plutusDataMapItem {
                                            mapItemKey = "visual_artist".toPlutusData()
                                            mapItemValue =
                                                if (visualArtists.size > 1) {
                                                    plutusData {
                                                        list =
                                                            plutusDataList {
                                                                listItem.addAll(
                                                                    visualArtists.map { collab ->
                                                                        UserEntity
                                                                            .getByEmail(collab.email!!)!!
                                                                            .toModel(false)
                                                                            .stageOrFullName
                                                                            .toPlutusData()
                                                                    }
                                                                )
                                                            }
                                                    }
                                                } else {
                                                    UserEntity
                                                        .getByEmail(visualArtists[0].email!!)!!
                                                        .toModel(false)
                                                        .stageOrFullName
                                                        .toPlutusData()
                                                }
                                        }
                                    )
                                }
                            }
                        }
                    }
            }
    }

    private fun createPlutusDataFiles(
        song: Song,
        user: User,
        collabs: List<Collaboration>
    ) = plutusDataMapItem {
        mapItemKey = "files".toPlutusData()
        mapItemValue =
            plutusData {
                list =
                    plutusDataList {
                        with(listItem) {
                            add(
                                plutusData {
                                    map =
                                        plutusDataMap {
                                            with(mapItem) {
                                                add(
                                                    plutusDataMapItem {
                                                        mapItemKey = "name".toPlutusData()
                                                        mapItemValue =
                                                            "Streaming Royalty Share Agreement".toPlutusData()
                                                    }
                                                )
                                                add(
                                                    plutusDataMapItem {
                                                        mapItemKey = "mediaType".toPlutusData()
                                                        mapItemValue = "application/pdf".toPlutusData()
                                                    }
                                                )
                                                add(
                                                    plutusDataMapItem {
                                                        mapItemKey = "src".toPlutusData()
                                                        mapItemValue =
                                                            song.arweaveTokenAgreementUrl!!.toPlutusData()
                                                    }
                                                )
                                            }
                                        }
                                }
                            )
                            add(
                                plutusData {
                                    map =
                                        plutusDataMap {
                                            with(mapItem) {
                                                add(
                                                    plutusDataMapItem {
                                                        mapItemKey = "name".toPlutusData()
                                                        mapItemValue = song.title!!.toPlutusData()
                                                    }
                                                )
                                                add(
                                                    plutusDataMapItem {
                                                        mapItemKey = "mediaType".toPlutusData()
                                                        mapItemValue = "audio/mpeg".toPlutusData()
                                                    }
                                                )
                                                add(
                                                    plutusDataMapItem {
                                                        mapItemKey = "src".toPlutusData()
                                                        mapItemValue = song.arweaveClipUrl!!.toPlutusData()
                                                    }
                                                )
                                                add(createPlutusDataSong(song, user, collabs))
                                            }
                                        }
                                }
                            )
                        }
                    }
            }
    }

    private fun createPlutusDataSong(
        song: Song,
        user: User,
        collabs: List<Collaboration>
    ) = plutusDataMapItem {
        mapItemKey = "song".toPlutusData()
        mapItemValue =
            plutusData {
                map =
                    plutusDataMap {
                        with(mapItem) {
                            add(
                                plutusDataMapItem {
                                    mapItemKey = "song_title".toPlutusData()
                                    mapItemValue = song.title!!.toPlutusData()
                                }
                            )
                            add(
                                plutusDataMapItem {
                                    mapItemKey = "song_duration".toPlutusData()
                                    mapItemValue =
                                        song.duration!!
                                            .milliseconds.inWholeSeconds.seconds
                                            .toIsoString()
                                            .toPlutusData()
                                }
                            )
                            add(
                                plutusDataMapItem {
                                    mapItemKey = "track_number".toPlutusData()
                                    mapItemValue = 1.toPlutusData()
                                }
                            )
                            song.moods?.takeUnless { it.isEmpty() }?.let { moods ->
                                add(
                                    plutusDataMapItem {
                                        mapItemKey = "mood".toPlutusData()
                                        mapItemValue =
                                            if (moods.size > 1) {
                                                plutusData {
                                                    list =
                                                        plutusDataList {
                                                            listItem.addAll(
                                                                moods.map { it.toPlutusData() }
                                                            )
                                                        }
                                                }
                                            } else {
                                                moods[0].toPlutusData()
                                            }
                                    }
                                )
                            }
                            add(createPlutusDataArtists(user, collabs))
                            if (!song.genres.isNullOrEmpty()) {
                                add(
                                    plutusDataMapItem {
                                        mapItemKey = "genres".toPlutusData()
                                        mapItemValue =
                                            plutusData {
                                                list =
                                                    plutusDataList {
                                                        listItem.addAll(song.genres.map { it.toPlutusData() })
                                                    }
                                            }
                                    }
                                )
                            }
                            add(
                                plutusDataMapItem {
                                    mapItemKey = "copyright".toPlutusData()
                                    mapItemValue = plutusData {
                                        map = plutusDataMap {
                                            with(mapItem) {
                                                add(
                                                    plutusDataMapItem {
                                                        mapItemKey = "master".toPlutusData()
                                                        mapItemValue =
                                                            "℗ ${song.phonographicCopyrightYear} ${song.phonographicCopyrightOwner}".toPlutusData()
                                                    }
                                                )
                                                add(
                                                    plutusDataMapItem {
                                                        mapItemKey = "composition".toPlutusData()
                                                        mapItemValue =
                                                            "© ${song.compositionCopyrightYear} ${song.compositionCopyrightOwner}".toPlutusData()
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            )
                            if (!song.arweaveLyricsUrl.isNullOrBlank()) {
                                add(
                                    plutusDataMapItem {
                                        mapItemKey = "lyrics".toPlutusData()
                                        mapItemValue = song.arweaveLyricsUrl.toPlutusData()
                                    }
                                )
                            }
                            if (!song.parentalAdvisory.isNullOrBlank()) {
                                add(
                                    plutusDataMapItem {
                                        mapItemKey = "parental_advisory".toPlutusData()
                                        mapItemValue = song.parentalAdvisory.toPlutusData()
                                    }
                                )
                                add(
                                    plutusDataMapItem {
                                        mapItemKey = "explicit".toPlutusData()
                                        mapItemValue =
                                            (!song.parentalAdvisory.equals("Non-Explicit", ignoreCase = true))
                                                .toString()
                                                .toPlutusData()
                                    }
                                )
                            }
                            add(
                                plutusDataMapItem {
                                    mapItemKey = "isrc".toPlutusData()
                                    mapItemValue = song.isrc!!.toPlutusData()
                                }
                            )
                            if (!song.iswc.isNullOrBlank()) {
                                add(
                                    plutusDataMapItem {
                                        mapItemKey = "iswc".toPlutusData()
                                        mapItemValue = song.iswc.toPlutusData()
                                    }
                                )
                            }
                            if (!song.ipis.isNullOrEmpty()) {
                                add(
                                    plutusDataMapItem {
                                        mapItemKey = "ipi".toPlutusData()
                                        mapItemValue =
                                            plutusData {
                                                list =
                                                    plutusDataList {
                                                        listItem.addAll(song.ipis.map { it.toPlutusData() })
                                                    }
                                            }
                                    }
                                )
                            }

                            // TODO: add country_of_origin from idenfy data? - maybe not MVP

                            val featuredArtists = collabs.filter { it.featured == true }
                            if (featuredArtists.isNotEmpty()) {
                                transaction {
                                    add(
                                        plutusDataMapItem {
                                            mapItemKey = "featured_artists".toPlutusData()
                                            mapItemValue = plutusData {
                                                list = plutusDataList {
                                                    listItem.addAll(
                                                        featuredArtists.map { collab ->
                                                            plutusData {
                                                                map = plutusDataMap {
                                                                    with(mapItem) {
                                                                        add(
                                                                            plutusDataMapItem {
                                                                                mapItemKey = "name".toPlutusData()
                                                                                mapItemValue =
                                                                                    UserEntity
                                                                                        .getByEmail(collab.email!!)!!
                                                                                        .toModel(false)
                                                                                        .stageOrFullName
                                                                                        .toPlutusData()
                                                                            }
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    )
                                }
                            }

                            val authors =
                                collabs.filter { it.roles?.containsIgnoreCase("Author (Lyrics)") == true && it.credited == true }
                            if (authors.isNotEmpty()) {
                                transaction {
                                    add(
                                        plutusDataMapItem {
                                            mapItemKey = "authors".toPlutusData()
                                            mapItemValue = plutusData {
                                                list = plutusDataList {
                                                    listItem.addAll(
                                                        authors.map { collab ->
                                                            plutusData {
                                                                map = plutusDataMap {
                                                                    with(mapItem) {
                                                                        add(
                                                                            plutusDataMapItem {
                                                                                mapItemKey = "name".toPlutusData()
                                                                                mapItemValue =
                                                                                    UserEntity
                                                                                        .getByEmail(collab.email!!)!!
                                                                                        .toModel(false)
                                                                                        .stageOrFullName
                                                                                        .toPlutusData()
                                                                            }
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    )
                                }
                            }

                            val contributingArtists =
                                collabs.filter {
                                    it.roles.orEmpty().any { ca -> ca in contributingArtistRoles } && it.credited == true
                                }
                            if (contributingArtists.isNotEmpty()) {
                                transaction {
                                    add(
                                        plutusDataMapItem {
                                            mapItemKey = "contributing_artists".toPlutusData()
                                            mapItemValue =
                                                plutusData {
                                                    list =
                                                        plutusDataList {
                                                            listItem.addAll(
                                                                contributingArtists.map { collab ->
                                                                    "${
                                                                        UserEntity.getByEmail(collab.email!!)!!
                                                                            .toModel(false).stageOrFullName
                                                                    }, ${collab.roles?.firstOrNull()}".toPlutusData()
                                                                }
                                                            )
                                                        }
                                                }
                                        }
                                    )
                                }
                            }

                            val mixEngineers =
                                collabs.filter { it.roles?.containsIgnoreCase("Mixing Engineer") == true && it.credited == true }
                            if (mixEngineers.isNotEmpty()) {
                                transaction {
                                    add(
                                        plutusDataMapItem {
                                            mapItemKey = "mix_engineer".toPlutusData()
                                            mapItemValue =
                                                if (mixEngineers.size > 1) {
                                                    plutusData {
                                                        list =
                                                            plutusDataList {
                                                                listItem.addAll(
                                                                    mixEngineers.map { collab ->
                                                                        UserEntity
                                                                            .getByEmail(collab.email!!)!!
                                                                            .toModel(false)
                                                                            .stageOrFullName
                                                                            .toPlutusData()
                                                                    }
                                                                )
                                                            }
                                                    }
                                                } else {
                                                    UserEntity
                                                        .getByEmail(mixEngineers[0].email!!)!!
                                                        .toModel(false)
                                                        .stageOrFullName
                                                        .toPlutusData()
                                                }
                                        }
                                    )
                                }
                            }

                            val masteringEngineers =
                                collabs.filter {
                                    it.roles?.containsIgnoreCase("Mastering Engineer") == true && it.credited == true
                                }
                            if (masteringEngineers.isNotEmpty()) {
                                transaction {
                                    add(
                                        plutusDataMapItem {
                                            mapItemKey = "mastering_engineer".toPlutusData()
                                            mapItemValue =
                                                if (masteringEngineers.size > 1) {
                                                    plutusData {
                                                        list =
                                                            plutusDataList {
                                                                listItem.addAll(
                                                                    masteringEngineers.map { collab ->
                                                                        UserEntity
                                                                            .getByEmail(collab.email!!)!!
                                                                            .toModel(false)
                                                                            .stageOrFullName
                                                                            .toPlutusData()
                                                                    }
                                                                )
                                                            }
                                                    }
                                                } else {
                                                    UserEntity
                                                        .getByEmail(masteringEngineers[0].email!!)!!
                                                        .toModel(false)
                                                        .stageOrFullName
                                                        .toPlutusData()
                                                }
                                        }
                                    )
                                }
                            }

                            val recordingEngineers =
                                collabs.filter {
                                    it.roles?.containsIgnoreCase("Recording Engineer") == true && it.credited == true
                                }
                            if (recordingEngineers.isNotEmpty()) {
                                transaction {
                                    add(
                                        plutusDataMapItem {
                                            mapItemKey = "recording_engineer".toPlutusData()
                                            mapItemValue =
                                                if (recordingEngineers.size > 1) {
                                                    plutusData {
                                                        list =
                                                            plutusDataList {
                                                                listItem.addAll(
                                                                    recordingEngineers.map { collab ->
                                                                        UserEntity
                                                                            .getByEmail(collab.email!!)!!
                                                                            .toModel(false)
                                                                            .stageOrFullName
                                                                            .toPlutusData()
                                                                    }
                                                                )
                                                            }
                                                    }
                                                } else {
                                                    UserEntity
                                                        .getByEmail(recordingEngineers[0].email!!)!!
                                                        .toModel(false)
                                                        .stageOrFullName
                                                        .toPlutusData()
                                                }
                                        }
                                    )
                                }
                            }

                            val producers =
                                collabs.filter { c ->
                                    c.roles.orEmpty().any { it in producerRoles } && c.credited == true
                                }
                            if (producers.isNotEmpty()) {
                                transaction {
                                    add(
                                        plutusDataMapItem {
                                            mapItemKey = "producer".toPlutusData()
                                            mapItemValue =
                                                if (producers.size > 1) {
                                                    plutusData {
                                                        list =
                                                            plutusDataList {
                                                                listItem.addAll(
                                                                    producers.map { collab ->
                                                                        UserEntity
                                                                            .getByEmail(collab.email!!)!!
                                                                            .toModel(false)
                                                                            .stageOrFullName
                                                                            .toPlutusData()
                                                                    }
                                                                )
                                                            }
                                                    }
                                                } else {
                                                    UserEntity
                                                        .getByEmail(producers[0].email!!)!!
                                                        .toModel(false)
                                                        .stageOrFullName
                                                        .toPlutusData()
                                                }
                                        }
                                    )
                                }
                            }
                        }
                    }
            }
    }

    private fun createPlutusDataArtists(
        user: User,
        collabs: List<Collaboration>
    ) = plutusDataMapItem {
        mapItemKey = "artists".toPlutusData()
        mapItemValue =
            plutusData {
                val primaryArtistEmails =
                    collabs
                        .filter { it.roles?.containsIgnoreCase("Artist") == true && it.credited == true && !(it.featured ?: false) }
                        .map { it.email!! }
                        .toMutableSet()
                if (primaryArtistEmails.isEmpty()) {
                    primaryArtistEmails.add(user.email!!)
                }
                list =
                    plutusDataList {
                        with(listItem) {
                            transaction {
                                primaryArtistEmails.forEach { email ->
                                    add(
                                        plutusData {
                                            map =
                                                plutusDataMap {
                                                    with(mapItem) {
                                                        add(
                                                            plutusDataMapItem {
                                                                mapItemKey = "name".toPlutusData()
                                                                mapItemValue =
                                                                    UserEntity
                                                                        .getByEmail(email)!!
                                                                        .toModel(false)
                                                                        .stageOrFullName
                                                                        .toPlutusData()
                                                            }
                                                        )
                                                    }
                                                }
                                        }
                                    )
                                }
                            }
                        }
                    }
            }
    }

    private fun createPlutusDataLinks(
        user: User,
        collabs: List<Collaboration>
    ) = transaction {
        plutusDataMapItem {
            val collabUsers =
                listOf(user) +
                    collabs
                        .filter { it.credited == true }
                        .mapNotNull { UserEntity.getByEmail(it.email!!)?.toModel(false) }

            val websites = collabUsers.mapNotNull { it.websiteUrl }.distinct()
            val instagrams = collabUsers.mapNotNull { it.instagramUrl }.distinct()
            val twitters = collabUsers.mapNotNull { it.twitterUrl }.distinct()

            mapItemKey = "links".toPlutusData()
            mapItemValue =
                plutusData {
                    map =
                        plutusDataMap {
                            with(mapItem) {
                                if (websites.isNotEmpty()) {
                                    add(
                                        plutusDataMapItem {
                                            mapItemKey = "website".toPlutusData()
                                            mapItemValue =
                                                if (websites.size > 1) {
                                                    plutusData {
                                                        list =
                                                            plutusDataList {
                                                                listItem.addAll(websites.map { it.toPlutusData() })
                                                            }
                                                    }
                                                } else {
                                                    websites[0].toPlutusData()
                                                }
                                        }
                                    )
                                }
                                if (instagrams.isNotEmpty()) {
                                    add(
                                        plutusDataMapItem {
                                            mapItemKey = "instagram".toPlutusData()
                                            mapItemValue =
                                                if (instagrams.size > 1) {
                                                    plutusData {
                                                        list =
                                                            plutusDataList {
                                                                listItem.addAll(instagrams.map { it.toPlutusData() })
                                                            }
                                                    }
                                                } else {
                                                    instagrams[0].toPlutusData()
                                                }
                                        }
                                    )
                                }
                                if (twitters.isNotEmpty()) {
                                    add(
                                        plutusDataMapItem {
                                            mapItemKey = "twitter".toPlutusData()
                                            mapItemValue =
                                                if (twitters.size > 1) {
                                                    plutusData {
                                                        list =
                                                            plutusDataList {
                                                                listItem.addAll(twitters.map { it.toPlutusData() })
                                                            }
                                                    }
                                                } else {
                                                    twitters[0].toPlutusData()
                                                }
                                        }
                                    )
                                }
                            }
                        }
                }
        }
    }

    companion object {
        // FIXME: these should come from the eveara api and we should flag our db with which ones are contributing roles
        private val contributingArtistRoles =
            listOf(
                "Composer (Music)",
                "Contributor",
                "Guitar",
                "Drums",
                "Bass",
                "Keyboards",
                "Vocal",
                "Harmonica",
                "Saxophone",
                "Violin",
                "Orchestra",
                "Organ",
                "Choir",
                "Piano",
                "Horns",
                "Strings",
                "Synthesizer",
                "Percussion"
            )

        private val producerRoles =
            listOf(
                "Producer",
                "Executive Producer"
            )

        private const val PREFIX_REF_TOKEN = "000643b0" // (100)
        private const val PREFIX_FRAC_TOKEN = "001bc280" // (444)
    }

    override suspend fun add(mintingStatusTransactionEntity: MintingStatusTransactionEntity) =
        transaction {
            MintingStatusTransactionEntity
                .new {
                    mintingStatus = mintingStatusTransactionEntity.mintingStatus
                    createdAt = mintingStatusTransactionEntity.createdAt
                    logMessage = mintingStatusTransactionEntity.logMessage
                    songId = mintingStatusTransactionEntity.songId
                }.id.value
        }

    override fun getMintingStatusHistoryEntity(songId: SongId): List<MintingStatusTransactionModel> {
        logger.debug { "get minting history for : songId = $songId" }
        return transaction {
            MintingStatusTransactionEntity
                .find(MintingStatusHistoryTable.songId eq songId)
                .map { it.toModel() }
        }
    }
}
