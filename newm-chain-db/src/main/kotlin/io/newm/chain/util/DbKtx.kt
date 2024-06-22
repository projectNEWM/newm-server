package io.newm.chain.util

import io.newm.chain.config.Config
import io.newm.chain.database.entity.ChainBlock
import io.newm.chain.database.entity.LedgerAsset
import io.newm.chain.database.entity.LedgerAssetMetadata
import io.newm.chain.database.entity.PaymentStakeAddress
import io.newm.chain.database.entity.RawTransaction
import io.newm.chain.database.entity.StakeDelegation
import io.newm.chain.database.entity.StakeRegistration
import io.newm.chain.model.CreatedUtxo
import io.newm.chain.model.NativeAsset
import io.newm.chain.model.SpentUtxo
import io.newm.chain.util.Constants.LEADER_VRF_HEADER
import io.newm.chain.util.Constants.NFT_METADATA_KEY
import io.newm.chain.util.Constants.NONCE_VRF_HEADER
import io.newm.chain.util.Constants.receiveAddressRegex
import io.newm.kogmios.protocols.model.Block
import io.newm.kogmios.protocols.model.BlockPraos
import io.newm.kogmios.protocols.model.Certificate
import io.newm.kogmios.protocols.model.MetadataBytes
import io.newm.kogmios.protocols.model.MetadataInteger
import io.newm.kogmios.protocols.model.MetadataList
import io.newm.kogmios.protocols.model.MetadataMap
import io.newm.kogmios.protocols.model.MetadataString
import io.newm.kogmios.protocols.model.MetadataValue
import io.newm.kogmios.protocols.model.ScriptPlutusV2
import io.newm.kogmios.protocols.model.StakeCredentialRegistrationCertificate
import io.newm.kogmios.protocols.model.StakeDelegationCertificate
import io.newm.kogmios.protocols.model.UtxoOutput
import org.slf4j.LoggerFactory

private val log by lazy { LoggerFactory.getLogger("DbKtx") }

fun Block.toChainBlock(): ChainBlock {
    require(this is BlockPraos) { "Block is not a Praos block" }
    return ChainBlock(
        blockNumber = this.height,
        slotNumber = this.slot,
        hash = this.id,
        prevHash = this.ancestor,
        nodeVkey = this.issuer.verificationKey,
        nodeVrfVkey = this.issuer.vrfVerificationKey,
        blockVrf = if (this.nonce == null) this.issuer.leaderValue.output else "",
        blockVrfProof = if (this.nonce == null) this.issuer.leaderValue.proof else "",
        etaVrf0 =
            this.nonce?.output
                ?: Blake2b
                    .hash256(
                        NONCE_VRF_HEADER +
                            this.issuer.leaderValue.output
                                .hexToByteArray()
                    ).toHexString(),
        etaVrf1 = this.nonce?.proof ?: "",
        leaderVrf0 =
            if (this.nonce != null) {
                this.issuer.leaderValue.output
            } else {
                Blake2b
                    .hash256(
                        LEADER_VRF_HEADER +
                            this.issuer.leaderValue.output
                                .hexToByteArray()
                    ).toHexString()
            },
        leaderVrf1 = if (this.nonce != null) this.issuer.leaderValue.proof else "",
        blockSize = this.size.bytes.toInt(),
        // this value is no longer sent by Ogmios 6
        blockBodyHash = "",
        poolOpcert = this.issuer.operationalCertificate.kes.verificationKey,
        sequenceNumber =
            this.issuer.operationalCertificate.count
                .toInt(),
        kesPeriod =
            this.issuer.operationalCertificate.kes.period
                .toInt(),
        // this value is no longer sent by Ogmios 6
        sigmaSignature = "",
        protocolMajorVersion = this.protocol.version.major,
        protocolMinorVersion = this.protocol.version.minor,
        transactionDestAddresses = this.toTransactionDestAddressSet(),
        stakeDestAddresses = this.toPaymentStakeAddressSet(),
    )
}

private fun Block.toTransactionDestAddressSet(): Set<String> {
    require(this is BlockPraos) { "Block is not a Praos block" }
    return this.transactions
        .flatMap { tx ->
            tx.outputs.map { utxoOutput ->
                utxoOutput.address
            }
        }.toSet()
}

private fun Block.toPaymentStakeAddressSet(): Set<PaymentStakeAddress> {
    require(this is BlockPraos) { "Block is not a Praos block" }
    return this.transactions
        .flatMap { tx ->
            tx.outputs.toPaymentStakeAddressList()
        }.toSet()
}

private fun List<UtxoOutput>.toPaymentStakeAddressList() =
    this.mapNotNull { utxoOutput ->
        if (receiveAddressRegex.matches(utxoOutput.address)) {
            PaymentStakeAddress(
                receivingAddress = utxoOutput.address,
                stakeAddress = utxoOutput.address.extractStakeAddress(Config.isMainnet),
            )
        } else {
            null
        }
    }

private fun UtxoOutput.extractStakeAddressOrNull() =
    if (receiveAddressRegex.matches(this.address)) {
        this.address.extractStakeAddress(Config.isMainnet)
    } else {
        null
    }

fun Block.toCreatedUtxoMap(): Map<String, Set<CreatedUtxo>> {
    require(this is BlockPraos) { "Block is not a Praos block" }
    return this.transactions.associate { transaction ->
        transaction.id to
            transaction.outputs.toCreatedUtxoList(transaction.id, transaction.datums.orEmpty()).toSet()
    }
}

fun Block.toCreatedUtxoSet(): Set<CreatedUtxo> {
    require(this is BlockPraos) { "Block is not a Praos block" }
    return this.transactions
        .flatMap { tx ->
            tx.outputs.toCreatedUtxoList(tx.id, tx.datums.orEmpty())
        }.toSet()
}

fun List<UtxoOutput>.toCreatedUtxoList(
    txId: String,
    datumsMap: Map<String, String>
) = this.mapIndexed { index, utxoOutput ->
    // Save off credential to txid mapping
    val credentials =
        if (utxoOutput.address.startsWith("addr")) {
            try {
                utxoOutput.address.extractCredentials()
            } catch (e: Throwable) {
                null
            }
        } else {
            null
        }

    val datumHash =
        utxoOutput.datumHash ?: utxoOutput.datum?.let { datum ->
            Blake2b.hash256(datum.hexToByteArray()).toHexString()
        }
    val datum =
        datumsMap[datumHash] ?: if (datumHash != utxoOutput.datum) {
            utxoOutput.datum
        } else {
            null
        }

    if (datum != null && datum == datumHash) {
        log.warn("Datum hash is the same as the datum itself: utxoOutput: $utxoOutput, datumsMap: $datumsMap")
    }

    CreatedUtxo(
        address = utxoOutput.address,
        addressType = utxoOutput.address.addressType(),
        stakeAddress = utxoOutput.extractStakeAddressOrNull(),
        hash = txId,
        ix = index.toLong(),
        lovelace = utxoOutput.value.ada.ada.lovelace,
        datumHash = datumHash,
        datum = datum,
        // we only care about plutus v2
        scriptRef = (utxoOutput.script as? ScriptPlutusV2)?.cbor,
        nativeAssets =
            utxoOutput.value.assets
                ?.map { asset ->
                    NativeAsset(
                        policy = asset.policyId,
                        name = asset.name,
                        amount = asset.quantity,
                    )
                }.orEmpty(),
        // TODO: fix if we ever need it
        cbor = null,
        paymentCred = credentials?.first,
        stakeCred = credentials?.second,
    )
}

fun Block.toSpentUtxoMap(): Map<String, Set<SpentUtxo>> {
    require(this is BlockPraos) { "Block is not a Praos block" }
    return this.transactions.associate { transaction ->
        transaction.id to
            transaction.inputs
                .map { utxoInput ->
                    SpentUtxo(
                        transactionSpent = transaction.id,
                        hash = utxoInput.transaction.id,
                        ix = utxoInput.index,
                    )
                }.toSet()
    }
}

fun Block.toSpentUtxoSet(): Set<SpentUtxo> {
    require(this is BlockPraos) { "Block is not a Praos block" }
    return this.transactions
        .flatMap { tx ->
            tx.inputs.map { utxoInput ->
                SpentUtxo(
                    transactionSpent = tx.id,
                    hash = utxoInput.transaction.id,
                    ix = utxoInput.index,
                )
            }
        }.toSet()
}

fun Block.toStakeRegistrationList(): List<StakeRegistration> {
    require(this is BlockPraos) { "Block is not a Praos block" }
    return this.transactions.flatMapIndexed { txIndex, transaction ->
        transaction.certificates.orEmpty().mapIndexedNotNull { certIndex, certificate ->
            certificate.toStakeRegistrationOrNull(this.slot, txIndex, certIndex)
        }
    }
}

private fun Certificate.toStakeRegistrationOrNull(
    slot: Long,
    txIndex: Int,
    certIndex: Int
): StakeRegistration? =
    when (this) {
        is StakeCredentialRegistrationCertificate ->
            StakeRegistration(
                slot = slot,
                txIndex = txIndex,
                certIndex = certIndex,
                stakeAddress = credential.credentialToStakeAddress(),
            )

        else -> null
    }

private fun String.credentialToStakeAddress(): String =
    if (Config.isMainnet) {
        Bech32.encode(
            "stake",
            ByteArray(1) { Constants.STAKE_ADDRESS_KEY_PREFIX_MAINNET } + this.hexToByteArray()
        )
    } else {
        Bech32.encode(
            "stake_test",
            ByteArray(1) { Constants.STAKE_ADDRESS_KEY_PREFIX_TESTNET } + this.hexToByteArray()
        )
    }

fun Block.toStakeDelegationList(epoch: Long): List<StakeDelegation> {
    require(this is BlockPraos) { "Block is not a Praos block" }
    return this.transactions.flatMap { transaction ->
        transaction.certificates.orEmpty().mapNotNull { certificate ->
            certificate.toStakeDelegationOrNull(this.height, epoch)
        }
    }
}

fun Block.toLedgerAssets(): List<LedgerAsset> {
    require(this is BlockPraos) { "Block is not a Praos block" }
    return this.transactions.flatMap { transaction ->
        transaction.mint
            ?.map { (policyId, nameAndAmountMap) ->
                nameAndAmountMap.map { (name, amount) ->
                    LedgerAsset(
                        policy = policyId,
                        name = name,
                        supply = amount,
                        txId = transaction.id,
                    )
                }
            }.orEmpty()
            .flatten()
    }
}

fun Block.toAssetMetadataList(ledgerAssets: List<LedgerAsset>): List<LedgerAssetMetadata> {
    require(this is BlockPraos) { "Block is not a Praos block" }
    return this.transactions.flatMap { transaction ->
        val assetMetadatas =
            transaction.metadata
                ?.labels
                ?.get(NFT_METADATA_KEY)
                ?.json as? MetadataMap
        assetMetadatas.extractAssetMetadata(ledgerAssets)
    }
}

fun MetadataMap?.extractAssetMetadata(ledgerAssets: List<LedgerAsset>): List<LedgerAssetMetadata> =
    this
        ?.flatMap { (policyIdKey, assetMetadataValue) ->
            val policyId =
                when (policyIdKey) {
                    is MetadataString -> policyIdKey.string
                    is MetadataBytes -> policyIdKey.bytes.b64ToByteArray().toHexString()
                    else -> null
                }
            policyId
                ?.let {
                    (assetMetadataValue as? MetadataMap)
                        ?.let { assetMetadataMap ->
                            assetMetadataMap.flatMap { (nameMetadataValue, detailsMetadataValue) ->
                                val name =
                                    when (nameMetadataValue) {
                                        is MetadataString -> nameMetadataValue.string
                                        is MetadataBytes -> nameMetadataValue.bytes.b64ToByteArray().toHexString()
                                        else -> null
                                    }
                                name
                                    ?.let {
                                        val hexName = it.toByteArray().toHexString()
                                        val ledgerAsset =
                                            ledgerAssets.find { asset ->
                                                asset.policy == policyId &&
                                                    (asset.name == hexName || asset.name == it)
                                            }

                                        ledgerAsset
                                            ?.let {
                                                (detailsMetadataValue as? MetadataMap)?.mapNotNull {
                                                        (keyMetadataValue, valueMetadataValue) ->
                                                    buildAssetMetadata(it.id!!, keyMetadataValue, valueMetadataValue, 0)
                                                }
                                            }.orEmpty()
                                    }.orEmpty()
                            }
                        }.orEmpty()
                }.orEmpty()
        }.orEmpty()

private fun buildAssetMetadata(
    assetId: Long,
    keyMetadataValue: MetadataValue,
    valueMetadataValue: MetadataValue,
    nestLevel: Int
): LedgerAssetMetadata? {
    val (key, keyType) =
        when (keyMetadataValue) {
            is MetadataString -> Pair(keyMetadataValue.string, "string")
            is MetadataBytes -> Pair(keyMetadataValue.bytes.b64ToByteArray().toHexString(), "bytestring")
            is MetadataInteger -> Pair(keyMetadataValue.int.toString(), "integer")
            else -> return null // ignore maps or lists for keys
        }

    val (value, valueType) =
        when (valueMetadataValue) {
            is MetadataString -> Pair(valueMetadataValue.string, "string")
            is MetadataBytes -> Pair(valueMetadataValue.bytes.b64ToByteArray().toHexString(), "bytestring")
            is MetadataInteger -> Pair(valueMetadataValue.int.toString(), "integer")
            is MetadataList -> Pair("", "array")
            is MetadataMap -> Pair("", "map")
        }

    return LedgerAssetMetadata(
        assetId = assetId,
        keyType = keyType,
        key = key,
        valueType = valueType,
        value = value,
        nestLevel = nestLevel,
        children =
            when (valueType) {
                "array" ->
                    (valueMetadataValue as MetadataList).mapNotNull { subValueMetadata ->
                        buildAssetMetadata(assetId, keyMetadataValue, subValueMetadata, nestLevel + 1)
                    }

                "map" ->
                    (valueMetadataValue as MetadataMap).mapNotNull { (subKeyMetadata, subValueMetadata) ->
                        buildAssetMetadata(assetId, subKeyMetadata, subValueMetadata, nestLevel + 1)
                    }

                else -> emptyList()
            }
    )
}

fun Block.toRawTransactionList(): List<RawTransaction> {
    require(this is BlockPraos) { "Block is not a Praos block" }
    return this.transactions.map { transaction ->
        val blockNumber = this.height
        val slotNumber = this.slot
        val blockSize = this.size.bytes.toInt()
        val blockBodyHash = "" // this value is no longer sent by Ogmios 6
        val protocolVersionMajor = this.protocol.version.major
        val protocolVersionMinor = this.protocol.version.minor

        RawTransaction(
            blockNumber = blockNumber,
            slotNumber = slotNumber,
            blockSize = blockSize,
            blockBodyHash = blockBodyHash,
            protocolVersionMajor = protocolVersionMajor,
            protocolVersionMinor = protocolVersionMinor,
            txId = transaction.id,
            tx = transaction.cbor!!.hexToByteArray(),
        )
    }
}

private fun Certificate.toStakeDelegationOrNull(
    blockNumber: Long,
    epoch: Long
): StakeDelegation? =
    when (this) {
        is StakeDelegationCertificate -> {
            this.stakePool?.id?.let { poolId ->
                StakeDelegation(
                    blockNumber = blockNumber,
                    stakeAddress = this.credential.credentialToStakeAddress(),
                    poolId = poolId,
                    epoch = epoch,
                )
            }
        }

        else -> null
    }
