package io.newm.chain.grpc

import com.google.iot.cbor.CborArray
import com.google.iot.cbor.CborByteString
import com.google.iot.cbor.CborInteger
import com.google.iot.cbor.CborMap
import com.google.iot.cbor.CborReader
import com.google.iot.cbor.CborSimple
import com.google.iot.cbor.CborTextString
import com.google.protobuf.kotlin.toByteString
import io.github.oshai.kotlinlogging.KotlinLogging
import io.newm.chain.cardano.address.Address
import io.newm.chain.cardano.address.AddressCredential
import io.newm.chain.cardano.address.BIP32PublicKey
import io.newm.chain.cardano.calculateTransactionId
import io.newm.chain.cardano.getCurrentEpoch
import io.newm.chain.cardano.toLedgerAssetMetadataItem
import io.newm.chain.config.Config
import io.newm.chain.database.repository.ChainRepository
import io.newm.chain.database.repository.LedgerRepository
import io.newm.chain.ledger.SubmittedTransactionCache
import io.newm.chain.model.toNativeAssetMap
import io.newm.chain.util.Bech32
import io.newm.chain.util.Blake2b
import io.newm.chain.util.Constants
import io.newm.chain.util.Constants.ROLE_CHANGE
import io.newm.chain.util.Constants.ROLE_PAYMENT
import io.newm.chain.util.Constants.ROLE_STAKING
import io.newm.chain.util.Constants.stakeAddressFinderRegex
import io.newm.chain.util.elementToByteArray
import io.newm.chain.util.hexToByteArray
import io.newm.chain.util.toCreatedUtxoMap
import io.newm.chain.util.toHexString
import io.newm.kogmios.StateQueryClient
import io.newm.kogmios.protocols.model.Block
import io.newm.kogmios.protocols.model.CardanoEra
import io.newm.kogmios.protocols.model.result.EvaluateTxResult
import io.newm.objectpool.useInstance
import io.newm.shared.koin.inject
import io.newm.txbuilder.TransactionBuilder
import io.newm.txbuilder.ktx.cborHexToPlutusData
import io.newm.txbuilder.ktx.toNativeAssetMap
import io.newm.txbuilder.ktx.verify
import io.newm.txbuilder.ktx.withMinUtxo
import io.sentry.Sentry
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.experimental.and
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.qualifier.named

class NewmChainService : NewmChainGrpcKt.NewmChainCoroutineImplBase() {
    private val log = KotlinLogging.logger {}
    private val chainRepository: ChainRepository by inject()
    private val ledgerRepository: LedgerRepository by inject()
    private val txSubmitClientPool: TxSubmitClientPool by inject()
    private val stateQueryClientPool: StateQueryClientPool by inject()
    private val submittedTransactionCache: SubmittedTransactionCache by inject()
    private val confirmedBlockFlow: MutableSharedFlow<Block> by inject(named("confirmedBlockFlow"))

    override suspend fun queryUtxos(request: QueryUtxosRequest): QueryUtxosResponse {
        try {
            return ledgerRepository.queryUtxos(request.address).toQueryUtxosResponse()
        } catch (e: Throwable) {
            Sentry.addBreadcrumb(request.toString(), "NewmChainService")
            log.error(e) { "queryUtxos error!" }
            throw e
        }
    }

    override suspend fun queryLiveUtxos(request: QueryUtxosRequest): QueryUtxosResponse {
        try {
            return ledgerRepository.queryLiveUtxos(request.address).toQueryUtxosResponse()
        } catch (e: Throwable) {
            Sentry.addBreadcrumb(request.toString(), "NewmChainService")
            log.error(e) { "queryLiveUtxos error!" }
            throw e
        }
    }

    override suspend fun queryUtxosByOutputRef(request: QueryUtxosOutputRefRequest): QueryUtxosResponse {
        try {
            return ledgerRepository.queryUtxosByOutputRef(request.hash, request.ix.toInt()).toQueryUtxosResponse()
        } catch (e: Throwable) {
            Sentry.addBreadcrumb(request.toString(), "NewmChainService")
            log.error(e) { "queryUtxosByOutputRef error!" }
            throw e
        }
    }

    override suspend fun queryPublicKeyHashByOutputRef(request: QueryUtxosOutputRefRequest): QueryPublicKeyHashResponse {
        try {
            return ledgerRepository.queryPublicKeyHashByOutputRef(request.hash, request.ix.toInt())?.let {
                queryPublicKeyHashResponse {
                    publicKeyHash = it
                }
            } ?: queryPublicKeyHashResponse { }
        } catch (e: Throwable) {
            Sentry.addBreadcrumb(request.toString(), "NewmChainService")
            log.error(e) { "queryPublicKeyHashByOutputRef error!" }
            throw e
        }
    }

    override suspend fun queryUtxosByStakeAddress(request: QueryUtxosRequest): QueryUtxosResponse {
        try {
            log.debug { "queryUtxosByStakeAddress(${request.address}) started." }
            val response: QueryUtxosResponse
            measureTimeMillis {
                response = ledgerRepository.queryUtxosByStakeAddress(request.address).toQueryUtxosResponse()
            }.also {
                log.debug { "queryUtxosByStakeAddress(${request.address}) completed in $it ms." }
            }
            return response
        } catch (e: Throwable) {
            Sentry.addBreadcrumb(request.toString(), "NewmChainService")
            log.error(e) { "queryUtxosByStakeAddress error!" }
            throw e
        }
    }

    private fun io.newm.chain.model.Utxo?.toRpcUtxo(): Utxo =
        this?.let { utxo ->
            utxo {
                address = utxo.address
                hash = utxo.hash
                ix = utxo.ix
                lovelace = utxo.lovelace.toString()
                utxo.datumHash?.let { datumHash = it }
                utxo.datum?.let {
                    datum = it.cborHexToPlutusData()
                }
                nativeAssets.addAll(
                    utxo.nativeAssets.map { nativeAsset ->
                        nativeAsset {
                            policy = nativeAsset.policy
                            name = nativeAsset.name
                            amount = nativeAsset.amount.toString()
                        }
                    }
                )
            }
        } ?: utxo { }

    private fun Set<io.newm.chain.model.Utxo>.toQueryUtxosResponse(): QueryUtxosResponse =
        queryUtxosResponse {
            utxos.addAll(
                this@toQueryUtxosResponse.map { utxo -> utxo.toRpcUtxo() }
            )
        }

    override suspend fun queryDatumByHash(request: QueryDatumByHashRequest): QueryDatumByHashResponse {
        try {
            return queryDatumByHashResponse {
                ledgerRepository.queryDatumByHash(request.datumHash)?.cborHexToPlutusData()?.let {
                    datum = it
                }
            }
        } catch (e: Throwable) {
            Sentry.addBreadcrumb(request.toString(), "NewmChainService")
            log.error(e) { "queryDatumByHash error!" }
            throw e
        }
    }

    override suspend fun queryPaymentAddressForStakeAddress(
        request: QueryPaymentAddressForStakeAddressRequest
    ): QueryPaymentAddressForStakeAddressResponse {
        try {
            return queryPaymentAddressForStakeAddressResponse {
                chainRepository.getPaymentAddressByStakeAddress(request.stakeAddress)?.let {
                    paymentAddress = it
                }
            }
        } catch (e: Throwable) {
            Sentry.addBreadcrumb(request.toString(), "NewmChainService")
            log.error(e) { "queryPaymentAddressForStakeAddress error!" }
            throw e
        }
    }

    override suspend fun queryCurrentEpoch(request: QueryCurrentEpochRequest): QueryCurrentEpochResponse {
        try {
            return queryCurrentEpochResponse {
                epoch = getCurrentEpoch()
            }
        } catch (e: Throwable) {
            Sentry.addBreadcrumb(request.toString(), "NewmChainService")
            log.error(e) { "queryCurrentEpoch error!" }
            throw e
        }
    }

    override suspend fun queryTransactionConfirmationCount(
        request: QueryTransactionConfirmationCountRequest
    ): QueryTransactionConfirmationCountResponse {
        try {
            return queryTransactionConfirmationCountResponse {
                txIdToConfirmationCount.putAll(
                    ledgerRepository.queryTransactionConfirmationCounts(request.txIdsList)
                )
            }
        } catch (e: Throwable) {
            Sentry.addBreadcrumb(request.toString(), "NewmChainService")
            log.error(e) { "queryTransactionConfirmationCount error!" }
            throw e
        }
    }

    override suspend fun submitTransaction(request: SubmitTransactionRequest): SubmitTransactionResponse =
        try {
            val cbor = request.cbor.toByteArray()
            txSubmitClientPool.useInstance { client ->
                val response = client.submit(cbor.toHexString())
                val transactionId = response.result.transaction.id
                if (submittedTransactionCache.get(transactionId) == null) {
                    submittedTransactionCache.put(transactionId, cbor)
                }

                ledgerRepository.updateLiveLedgerState(transactionId, cbor)

                submitTransactionResponse {
                    result = "MsgAcceptTx"
                    txId = transactionId
                }.also {
                    log.warn { "submitTransaction SUCCESS. txId: ${it.txId}" }
                }
            }
        } catch (e: Throwable) {
            Sentry.addBreadcrumb(request.toString(), "NewmChainService")
            log.error(e) { "submitTransaction(${request.cbor.toByteArray().toHexString()}) failed." }
            submitTransactionResponse {
                result =
                    "submitTransaction(${request.cbor.toByteArray().toHexString()}) failed. Exception: ${e.message}"
            }
        }

    override fun monitorAddress(request: MonitorAddressRequest): Flow<MonitorAddressResponse> {
        log.warn { "monitorAddress($request) started." }
        val responseFlow = MutableSharedFlow<MonitorAddressResponse>(replay = 1)
        val messageHandlerJob: Job =
            CoroutineScope(context).launch {
                try {
                    var startAfterTxId: String? =
                        if (request.hasStartAfterTxId()) {
                            request.startAfterTxId.trim()
                        } else {
                            null
                        }
                    val limit = 1000
                    var offset = 0L
                    var nextStartAfterTxId: String? = startAfterTxId
                    while (true) {
                        while (true) {
                            val monitorAddressResponseList =
                                ledgerRepository
                                    .queryAddressTxLogsAfter(
                                        request.address.trim(),
                                        startAfterTxId,
                                        limit,
                                        offset,
                                    ).map {
                                        MonitorAddressResponse.parseFrom(it)
                                    }

                            if (monitorAddressResponseList.isNotEmpty()) {
                                monitorAddressResponseList.forEach { monitorAddressResponse ->
                                    responseFlow.emit(monitorAddressResponse)
                                }
                                offset += monitorAddressResponseList.size
                                nextStartAfterTxId = monitorAddressResponseList.last().txId
                            } else {
                                break
                            }
                        }

                        startAfterTxId = nextStartAfterTxId
                        offset = 0L
                        delay(1000L)
                    }
                } catch (e: Throwable) {
                    if (e !is CancellationException) {
                        log.error(e) { e.message }
                    }
                }
            }

        messageHandlerJob.invokeOnCompletion {
            log.warn { "invokeOnCompletion: ${it?.message}" }
        }
        return responseFlow
    }

    override suspend fun isMainnet(request: IsMainnetRequest): IsMainnetResponse =
        isMainnetResponse {
            isMainnet = Config.isMainnet
        }

    override suspend fun queryCardanoEra(request: CardanoEraRequest): CardanoEraResponse =
        cardanoEraResponse {
            era =
                stateQueryClientPool.useInstance { client ->
                    io.newm.chain.grpc.CardanoEra
                        .valueOf(client.health().currentEra.name)
                }
        }

    override suspend fun monitorPaymentAddress(request: MonitorPaymentAddressRequest): MonitorPaymentAddressResponse {
        try {
            val requestNativeAssetMap =
                request.nativeAssetsList
                    .map {
                        io.newm.chain.model.NativeAsset(
                            policy = it.policy,
                            name = it.name,
                            amount = it.amount.toBigInteger()
                        )
                    }.toNativeAssetMap()

            return withTimeoutOrNull(request.timeoutMs) {
                // Check utxos immediately to see if payment has already arrived
                val liveUtxosResponse = queryLiveUtxos(queryUtxosRequest { address = request.address })
                val existingUtxo =
                    liveUtxosResponse.utxosList.firstOrNull { utxo ->
                        (utxo.lovelace == request.lovelace) &&
                            // lovelace matches
                            (requestNativeAssetMap == utxo.nativeAssetsList.toNativeAssetMap()) // nativeAssets match exactly
                    }
                if (existingUtxo != null) {
                    monitorPaymentAddressResponse {
                        success = true
                        message = "Payment Received"
                    }
                } else {
                    // Wait for payment to arrive
                    val matchingUtxo =
                        confirmedBlockFlow
                            .mapNotNull { block ->
                                block.toCreatedUtxoMap().values.flatten().firstOrNull { createdUtxo ->
                                    (createdUtxo.address == request.address) &&
                                        // address matches
                                        (createdUtxo.lovelace == request.lovelace.toBigInteger()) &&
                                        // lovelace matches
                                        (requestNativeAssetMap == createdUtxo.nativeAssets.toNativeAssetMap()) // nativeAssets match exactly
                                }
                            }.firstOrNull()

                    matchingUtxo?.let {
                        // Make sure this utxo is not spent by checking liveUtxos on the address
                        val response = queryLiveUtxos(queryUtxosRequest { address = request.address })
                        val lovelace = it.lovelace.toString()
                        val reqNativeAssetMap = request.nativeAssetsList.toNativeAssetMap()
                        response.utxosList.firstOrNull { utxo ->
                            (utxo.hash == it.hash) &&
                                (utxo.ix == it.ix) &&
                                (utxo.lovelace == lovelace) &&
                                (reqNativeAssetMap == utxo.nativeAssetsList.toNativeAssetMap())
                        }

                        monitorPaymentAddressResponse {
                            success = true
                            message = "Payment Received"
                        }
                    }
                }
            } ?: monitorPaymentAddressResponse {
                success = false
                message = "Timeout waiting for payment to arrive!"
            }
        } catch (e: Throwable) {
            Sentry.addBreadcrumb(request.toString(), "NewmChainService")
            log.error(e) { "monitorPaymentAddress error!" }
            throw e
        }
    }

    override suspend fun transactionBuilder(request: TransactionBuilderRequest): TransactionBuilderResponse =
        try {
            txSubmitClientPool.useInstance { txSubmitClient ->
                val stateQueryClient = txSubmitClient as StateQueryClient
                val protocolParams = stateQueryClient.protocolParameters().result
                val calculateTxExecutionUnits: suspend (ByteArray) -> EvaluateTxResult = { cborBytes ->
                    // FIXME: We should use PlutoK, Aiken, or some other way to calculate the execution units without relying on Ogmios.
                    // log.warn { "EvaluateTx: ${cborBytes.toHexString()}" }
                    try {
                        txSubmitClient.evaluate(cborBytes.toHexString()).result
                    } catch (e: Throwable) {
                        log.error { "EvaluateTx error: ${e.message}, cbor: ${cborBytes.toHexString()}" }
                        throw e
                    }
                }
                val calculateReferenceScriptBytes: suspend (Set<Utxo>) -> Long = { utxos ->
                    if (stateQueryClient.health().currentEra.ordinal < CardanoEra.CONWAY.ordinal) {
                        // Not yet in Conway era, reference scripts don't cost anything
                        0L
                    } else {
                        utxos.sumOf { utxo ->
                            val scriptRefHexLength =
                                requireNotNull(
                                    ledgerRepository
                                        .queryUtxosByOutputRef(utxo.hash, utxo.ix.toInt())
                                        .firstOrNull()
                                ) { "Utxo not found in ledger: ${utxo.hash}#${utxo.ix}" }
                                    .scriptRef
                                    ?.length
                                    ?.toLong()
                                    ?: 0L
                            // divide by 2 to get the ByteSize since the scriptRef is hex encoded
                            scriptRefHexLength / 2L
                        }
                    }
                }

                val calculateReferenceScriptsVersions: suspend (Set<Utxo>) -> Set<Int> = { utxos ->
                    utxos
                        .mapNotNull { utxo ->
                            val firstUtxo =
                                ledgerRepository.queryUtxosByOutputRef(utxo.hash, utxo.ix.toInt()).firstOrNull()
                            firstUtxo?.scriptRefVersion ?: firstUtxo?.scriptRef?.let { scriptRef ->
                                if (scriptRef.startsWith("010100")) {
                                    3
                                } else {
                                    2
                                }
                            }
                        }.toSet()
                }

                val updatedRequest =
                    if (request.signaturesCount == 0 && request.signingKeysCount == 0 && request.requiredSignersCount == 0) {
                        // Calculate the number of different payment keys associated with all input utxos
                        val requiredSigners =
                            (request.sourceUtxosList + request.collateralUtxosList)
                                .mapNotNull { utxo ->
                                    ledgerRepository.queryPublicKeyHashByOutputRef(utxo.hash, utxo.ix.toInt())
                                }.distinct()
                                .map {
                                    it.hexToByteArray().toByteString()
                                }
                        request
                            .toBuilder()
                            .addAllRequiredSigners(requiredSigners)
                            .build()
                    } else {
                        request
                    }

                val cardanoEra =
                    if (updatedRequest.hasEra()) {
                        CardanoEra.valueOf(updatedRequest.era.name)
                    } else {
                        CardanoEra.CONWAY
                    }

                val (txId, cborBytes) =
                    TransactionBuilder.transactionBuilder(
                        protocolParams,
                        cardanoEra,
                        calculateTxExecutionUnits,
                        calculateReferenceScriptBytes,
                        calculateReferenceScriptsVersions,
                    ) {
                        loadFrom(updatedRequest)
                    }
                transactionBuilderResponse {
                    transactionId = txId
                    transactionCbor = cborBytes.toByteString()
                }
            }
        } catch (e: Throwable) {
            Sentry.addBreadcrumb(request.toString(), "NewmChainService")
            log.error(e) { "TransactionBuilder error!" }
            transactionBuilderResponse {
                transactionId = ""
                transactionCbor = ByteArray(0).toByteString()
                errorMessage = e.message ?: "TransactionBuilder error!: $e"
            }
        }

    override fun monitorNativeAssets(request: MonitorNativeAssetsRequest): Flow<MonitorNativeAssetsResponse> {
        log.info { "monitorNativeAssets request: $request" }
        val responseFlow = MutableSharedFlow<MonitorNativeAssetsResponse>(replay = 1)
        val messageHandlerJob: Job =
            CoroutineScope(context).launch {
                try {
                    var startAfterId: Long? =
                        if (request.hasStartAfterId()) {
                            request.startAfterId
                        } else {
                            null
                        }
                    val limit = 1000
                    var offset = 0L
                    var nativeAssetLogList: List<Pair<Long, ByteArray>>
                    var nextStartAfterId: Long = startAfterId ?: -1L
                    while (true) {
                        while (true) {
                            // loop through all existing records before we change startAfterId
                            nativeAssetLogList = ledgerRepository.queryNativeAssetLogsAfter(startAfterId, limit, offset)

                            if (nativeAssetLogList.isNotEmpty()) {
                                nativeAssetLogList.forEach { nativeAssetLog ->
                                    responseFlow.emit(
                                        MonitorNativeAssetsResponse
                                            .parseFrom(nativeAssetLog.second)
                                            .toBuilder()
                                            .setId(nativeAssetLog.first)
                                            .build()
                                    )
                                }
                                offset += nativeAssetLogList.size
                                nextStartAfterId = nativeAssetLogList.last().first
                            } else {
                                break
                            }
                        }

                        startAfterId = nextStartAfterId
                        offset = 0L
                        delay(1000L)
                    }
                } catch (e: Throwable) {
                    if (e !is CancellationException) {
                        Sentry.addBreadcrumb(request.toString(), "NewmChainService")
                        log.error(e) { e.message }
                    }
                }
            }

        messageHandlerJob.invokeOnCompletion {
            log.warn { "invokeOnCompletion: ${it?.message}" }
        }
        return responseFlow
    }

    override suspend fun calculateMinUtxoForOutput(request: OutputUtxo): OutputUtxo {
        try {
            return txSubmitClientPool.useInstance { txSubmitClient ->
                val stateQueryClient = txSubmitClient as StateQueryClient
                val protocolParams = stateQueryClient.protocolParameters().result
                request.withMinUtxo(protocolParams.minUtxoDepositCoefficient.toLong())
            }
        } catch (e: Throwable) {
            Sentry.addBreadcrumb(request.toString(), "NewmChainService")
            log.error(e) { "calculateMinUtxoForOutput error!" }
            throw e
        }
    }

    override suspend fun snapshotNativeAssets(request: SnapshotNativeAssetsRequest): SnapshotNativeAssetsResponse {
        try {
            val snapshotEntries: List<SnapshotEntry> =
                ledgerRepository.snapshotNativeAssets(request.policy, request.name).map { (stakeAddress, amount) ->
                    snapshotEntry {
                        this.stakeAddress = stakeAddress
                        this.amount = amount
                    }
                }
            return snapshotNativeAssetsResponse {
                this.snapshotEntries.addAll(snapshotEntries)
            }
        } catch (e: Throwable) {
            Sentry.addBreadcrumb(request.toString(), "NewmChainService")
            log.error(e) { "snapshotNativeAssets error!" }
            throw e
        }
    }

    override suspend fun deriveWalletAddresses(request: WalletRequest): DeriveWalletAddressesResponse {
        try {
            // m / purpose' / coin_type' / account' / role / index
            // Example: m / 1852' / 1815' / 0' / 0 / 0
            // The user has sent us the account public key, so we can derive all roles/index values below that

            val rootAccountPk = BIP32PublicKey(bech32XPub = request.accountXpubKey)
            val stakePk = rootAccountPk.derive(ROLE_STAKING).derive(0u)
            val stakeCredential = AddressCredential.fromKey(stakePk)
            val stakeAddress = Address.fromStakeAddressCredential(stakeCredential, Config.isMainnet).address

            val paymentRootPk = rootAccountPk.derive(ROLE_PAYMENT)
            val enterpriseAddresses = deriveAddresses(ROLE_PAYMENT, paymentRootPk)
            val paymentStakeAddresses = deriveAddresses(ROLE_PAYMENT, paymentRootPk, stakeCredential)
            val changeRootPk = rootAccountPk.derive(ROLE_CHANGE)
            val enterpriseChangeAddresses = deriveAddresses(ROLE_CHANGE, changeRootPk)
            val paymentStakeChangeAddresses = deriveAddresses(ROLE_CHANGE, changeRootPk, stakeCredential)

            return deriveWalletAddressesResponse {
                this.stakeAddress =
                    address {
                        this.address = stakeAddress
                        this.role = ROLE_STAKING.toInt()
                        this.index = 0
                        this.used = true
                    }
                this.enterpriseAddress.addAll(enterpriseAddresses)
                this.paymentStakeAddress.addAll(paymentStakeAddresses)
                this.enterpriseChangeAddress.addAll(enterpriseChangeAddresses)
                this.paymentStakeChangeAddress.addAll(paymentStakeChangeAddresses)
            }
        } catch (e: Throwable) {
            Sentry.addBreadcrumb(request.toString(), "NewmChainService")
            log.error(e) { "deriveWalletAddresses error!" }
            throw e
        }
    }

    private fun deriveAddresses(
        role: UInt,
        rolePk: BIP32PublicKey,
        stakeCredential: AddressCredential? = null
    ): List<io.newm.chain.grpc.Address> {
        val addresses = mutableListOf<io.newm.chain.grpc.Address>()
        var indexStart = 0u
        do {
            val paymentAddresses =
                (indexStart..indexStart + 39u).map { index ->
                    val paymentPk = rolePk.derive(index)
                    val paymentCredential = AddressCredential.fromKey(paymentPk)
                    stakeCredential?.let {
                        Address
                            .fromPaymentStakeAddressCredentialsKeyKey(
                                paymentCredential,
                                it,
                                Config.isMainnet
                            ).address
                    } ?: Address.fromPaymentAddressCredential(paymentCredential, Config.isMainnet).address
                }
            val usedAddresses = ledgerRepository.queryUsedAddresses(paymentAddresses)
            addresses.addAll(
                paymentAddresses.mapIndexed { index, address ->
                    address {
                        this.address = address
                        this.role = role.toInt()
                        this.index = (indexStart + index.toUInt()).toInt()
                        this.used = address in usedAddresses
                    }
                }
            )

            indexStart += 40u
        } while (usedAddresses.isNotEmpty())
        return addresses
    }

    override suspend fun queryWalletControlledLiveUtxos(request: WalletRequest): QueryWalletControlledUtxosResponse {
        try {
            // attempt decoding to ensure it's a valid bech32 xpub
            require(Bech32.decode(request.accountXpubKey).bytes.size == 64) { "Invalid accountXpubKey: ${request.accountXpubKey}" }

            val rootAccountPk = BIP32PublicKey(bech32XPub = request.accountXpubKey)
            val stakePk = rootAccountPk.derive(ROLE_STAKING).derive(0u)
            val stakeCredential = AddressCredential.fromKey(stakePk)

            val paymentRootPk = rootAccountPk.derive(ROLE_PAYMENT)
            val enterpriseAddresses = deriveAddresses(ROLE_PAYMENT, paymentRootPk).filter { it.used }
            val paymentStakeAddresses = deriveAddresses(ROLE_PAYMENT, paymentRootPk, stakeCredential).filter { it.used }
            val changeRootPk = rootAccountPk.derive(ROLE_CHANGE)
            val enterpriseChangeAddresses = deriveAddresses(ROLE_CHANGE, changeRootPk).filter { it.used }
            val paymentStakeChangeAddresses =
                deriveAddresses(ROLE_CHANGE, changeRootPk, stakeCredential).filter { it.used }

            val allUsedAddresses =
                enterpriseAddresses + paymentStakeAddresses + enterpriseChangeAddresses + paymentStakeChangeAddresses

            val addressUtxosList =
                allUsedAddresses.mapNotNull { address ->
                    val utxos = ledgerRepository.queryLiveUtxos(address.address)
                    if (utxos.isEmpty()) {
                        null
                    } else {
                        addressUtxos {
                            this.address = address
                            this.utxos.addAll(utxos.toQueryUtxosResponse().utxosList)
                        }
                    }
                }

            return queryWalletControlledUtxosResponse {
                this.addressUtxos.addAll(addressUtxosList)
            }
        } catch (e: Throwable) {
            Sentry.addBreadcrumb(request.toString(), "NewmChainService")
            log.error(e) { "queryWalletControlledLiveUtxos error!" }
            return queryWalletControlledUtxosResponse {
                errorMessage = e.message ?: "queryWalletControlledLiveUtxos error!: $e"
            }
        }
    }

    override suspend fun queryUtxoByNativeAsset(request: QueryByNativeAssetRequest): Utxo {
        try {
            return ledgerRepository.queryUtxoByNativeAsset(request.name, request.policy).toRpcUtxo()
        } catch (e: Throwable) {
            Sentry.addBreadcrumb(request.toString(), "NewmChainService")
            log.error(e) { "queryUtxoByNativeAsset error!" }
            throw e
        }
    }

    override suspend fun queryLiveUtxoByNativeAsset(request: QueryByNativeAssetRequest): Utxo {
        try {
            return ledgerRepository.queryLiveUtxoByNativeAsset(request.name, request.policy).toRpcUtxo()
        } catch (e: Throwable) {
            Sentry.addBreadcrumb(request.toString(), "NewmChainService")
            log.error(e) { "queryLiveUtxoByNativeAsset error!" }
            throw e
        }
    }

    private val mutexMap = ConcurrentHashMap<String, Mutex>()
    private val mutexOwnerMap = ConcurrentHashMap<String, UUID>()
    private val mutexExpiryJobMap = ConcurrentHashMap<String, Job>()

    override suspend fun acquireMutex(request: AcquireMutexRequest): MutexResponse {
        try {
            val mutex = mutexMap.getOrPut(request.mutexName) { Mutex() }
            val locked =
                withTimeoutOrNull(request.acquireWaitTimeoutMs) {
                    val owner = mutexOwnerMap.getOrPut(request.mutexName) { UUID.randomUUID() }
                    mutex.lock(owner)
                    true
                } ?: false

            return if (locked) {
                mutexExpiryJobMap[request.mutexName] =
                    CoroutineScope(context).launch {
                        delay(request.lockExpiryMs)
                        // There's a highly-unlikely race condition with unlocking the mutex here.
                        // If the mutex is also unlocked by the releaseMutex request at the same moment, then this will throw an exception.
                        mutex.unlock(request.mutexName)
                        mutexExpiryJobMap.remove(request.mutexName)
                    }
                mutexResponse {
                    success = true
                    message = "Mutex acquired!"
                }
            } else {
                mutexResponse {
                    success = false
                    message = "Mutex acquire timeout!"
                }
            }
        } catch (e: Throwable) {
            Sentry.addBreadcrumb(request.toString(), "NewmChainService")
            log.error(e) { "acquireMutex error!" }
            throw e
        }
    }

    override suspend fun releaseMutex(request: ReleaseMutexRequest): MutexResponse {
        try {
            val mutex = mutexMap[request.mutexName]
            return if (mutex != null) {
                mutexExpiryJobMap.remove(request.mutexName)?.cancel()
                // There's a highly-unlikely race condition with unlocking the mutex here.
                // If the mutex is also unlocked by the expiry job at the same moment, then this will throw an exception.
                val owner = mutexOwnerMap.remove(request.mutexName)
                mutex.unlock(owner)
                mutexResponse {
                    success = true
                    message = "Mutex released!"
                }
            } else {
                mutexResponse {
                    success = false
                    message = "Mutex not found!"
                }
            }
        } catch (e: Throwable) {
            Sentry.addBreadcrumb(request.toString(), "NewmChainService")
            log.error(e) { "releaseMutex error!" }
            throw e
        }
    }

    override suspend fun ping(request: PingRequest): PongResponse =
        pongResponse {
            message = request.message
        }

    override suspend fun queryLedgerAssetMetadataListByNativeAsset(request: QueryByNativeAssetRequest): LedgerAssetMetadataListResponse {
        try {
            return ledgerRepository.queryLedgerAssetMetadataListByNativeAsset(request.name, request.policy).let {
                ledgerAssetMetadataListResponse {
                    this.ledgerAssetMetadata.addAll(it.map { ledgerAssetMetadata -> ledgerAssetMetadata.toLedgerAssetMetadataItem() })
                }
            }
        } catch (e: Throwable) {
            Sentry.addBreadcrumb(request.toString(), "NewmChainService")
            log.error(e) { "queryLedgerAssetMetadataListByNativeAsset error!" }
            throw e
        }
    }

    override suspend fun verifySignData(request: VerifySignDataRequest): VerifySignDataResponse {
        try {
            // process the COSE_Sign1 data
            val sigData =
                CborReader.createFromByteArray(request.signatureHex.hexToByteArray()).readDataItem() as CborArray
            require(sigData.size() == 4) { "Invalid COSE_Sign1 data! It must be an array of size 4" }

            // process the COSE_Sign1 protected header
            val protectedHeader =
                CborReader.createFromByteArray(sigData.elementToByteArray(0)).readDataItem() as CborMap
            val protectedHeaderAlgorithm = protectedHeader.get(CborInteger.create(1)) as CborInteger
            require(protectedHeaderAlgorithm.intValueExact() == -8) { "Invalid COSE_Sign1 algorithm! It must be -8 (EdDSA)" }
            val protectedHeaderStakeAddressBytes =
                (protectedHeader.get("address") as CborByteString).byteArrayValue()[0]
            require(
                protectedHeaderStakeAddressBytes[0] == Constants.STAKE_ADDRESS_KEY_PREFIX_TESTNET ||
                    protectedHeaderStakeAddressBytes[0] == Constants.STAKE_ADDRESS_KEY_PREFIX_MAINNET
            ) {
                "Invalid stake address prefix! : ${protectedHeaderStakeAddressBytes.sliceArray(0..0).toHexString()}"
            }
            val protectedHeaderStakeAddress =
                if (protectedHeaderStakeAddressBytes[0] == Constants.STAKE_ADDRESS_KEY_PREFIX_TESTNET) {
                    Bech32.encode("stake_test", protectedHeaderStakeAddressBytes)
                } else {
                    Bech32.encode("stake", protectedHeaderStakeAddressBytes)
                }

            // process the COSE_Sign1 unprotected header
            val unprotectedHeader = sigData.elementAt(1) as CborMap
            val hashed = unprotectedHeader.get("hashed") as CborSimple
            require(hashed.equals(CborSimple.TRUE) || hashed.equals(CborSimple.FALSE)) {
                "Invalid COSE_Sign1 unprotected header! hashed must be boolean!"
            }

            // process the COSE_Sign1 payload
            val challengeBytes = sigData.elementToByteArray(2)
            val challengeString = String(challengeBytes, Charsets.UTF_8)
            val challengeStakeAddress =
                requireNotNull(stakeAddressFinderRegex.find(challengeString)?.value) { "No stake address found in challenge!" }
            require(
                challengeStakeAddress == protectedHeaderStakeAddress
            ) { "Stake address mismatch!, challenge: $challengeStakeAddress, header: $protectedHeaderStakeAddress" }

            val signature1Payload =
                CborArray
                    .create()
                    .apply {
                        // context
                        add(CborTextString.create("Signature1"))
                        // protected header cbor
                        add(CborByteString.create(sigData.elementToByteArray(0)))
                        // sign protected (not present in this case)
                        add(CborByteString.create(ByteArray(0)))
                        // payload data bytes
                        add(CborByteString.create(challengeBytes))
                    }.toCborByteArray()

            // process the COSE_Sign1 key
            val coseKey =
                CborReader.createFromByteArray(request.publicKeyHex.hexToByteArray()).readDataItem() as CborMap
            val kty = coseKey.get(CborInteger.create(1)) as CborInteger
            require(kty.intValueExact() == 1) { "Invalid COSE_Sign1 key! kty must be 1 (OKP)" }
            val alg = coseKey.get(CborInteger.create(3)) as CborInteger
            require(alg.intValueExact() == -8) { "Invalid COSE_Sign1 key! alg must be -8 (EdDSA)" }
            val crv = coseKey.get(CborInteger.create(-1)) as CborInteger
            require(crv.intValueExact() == 6) { "Invalid COSE_Sign1 key! crv must be 6 (Ed25519)" }
            val x = (coseKey.get(CborInteger.create(-2)) as CborByteString).byteArrayValue()[0]
            require(x.size == 32) { "Invalid COSE_Sign1 key! x must be 32 bytes" }

            val stakeAddressFromPublicKey =
                if (challengeStakeAddress.startsWith("stake_test", ignoreCase = true)) {
                    // testnet stake key
                    Bech32.encode(
                        "stake_test",
                        ByteArray(1) { Constants.STAKE_ADDRESS_KEY_PREFIX_TESTNET } + Blake2b.hash224(x)
                    )
                } else {
                    // mainnet stake key
                    Bech32.encode(
                        "stake",
                        ByteArray(1) { Constants.STAKE_ADDRESS_KEY_PREFIX_MAINNET } + Blake2b.hash224(x)
                    )
                }
            require(protectedHeaderStakeAddress == stakeAddressFromPublicKey) {
                "Stake address mismatch!, header: $protectedHeaderStakeAddress, calculated: $stakeAddressFromPublicKey"
            }

            // verify the signature
            val publicKey =
                signingKey {
                    vkey = x.toByteString()
                }
            val signatureBytes = sigData.elementToByteArray(3)
            val isVerified = publicKey.verify(signature1Payload, signatureBytes)
            return verifySignDataResponse {
                verified = isVerified
                challenge = challengeString
            }
        } catch (e: Throwable) {
            Sentry.addBreadcrumb(request.toString(), "NewmChainService")
            log.error(e) { "verifySignData error!" }
            return verifySignDataResponse {
                verified = false
                challenge = ""
                errorMessage = "verifySignData error!: $e"
            }
        }
    }

    override suspend fun verifySignTransaction(request: SubmitTransactionRequest): VerifySignDataResponse {
        try {
            val cbor = request.cbor.toByteArray()
            val tx = CborReader.createFromByteArray(cbor).readDataItem() as CborArray
            val txBody = tx.elementAt(0) as CborMap
            val txId = calculateTransactionId(txBody)
            val txAuxDataHash = (txBody.get(CborInteger.create(7)) as CborByteString).byteArrayValue()[0].toHexString()
            val txAuxData = tx.elementAt(3) as? CborMap
            requireNotNull(txAuxData) { "Invalid transaction! txAuxData is missing!" }
            require(txAuxData.tag == 259) { "Invalid aux data tag! It must be 259" }
            require(
                Blake2b.hash256(txAuxData.toCborByteArray()).toHexString() == txAuxDataHash
            ) { "Invalid aux data hash!" }

            // extract the challengeString from the metadata so we can return it later
            val txMetadata = txAuxData.get(CborInteger.create(0)) as? CborMap
            requireNotNull(txMetadata) { "Invalid transaction! txMetadata is missing!" }
            val challengeString =
                (txMetadata.get(CborInteger.create(674)) as? CborMap)?.let { metadataMap ->
                    (metadataMap.get("msg") as? CborArray)?.let { msgArray ->
                        msgArray.joinToString("") { (it as CborTextString).stringValue() }
                    }
                }
            requireNotNull(challengeString) { "Invalid transaction! challengeString is missing!" }

            val challengeStakeAddress =
                requireNotNull(stakeAddressFinderRegex.find(challengeString)?.value) { "No stake address found in challenge!" }
            val challengeStakeAddressHeaderByte = Bech32.decode(challengeStakeAddress).bytes[0]
            val isMainnet = (challengeStakeAddressHeaderByte and 0x01.toByte() == 0x01.toByte())

            // process the required signer stake address
            val requiredSignerBytes = (txBody.get(CborInteger.create(14)) as? CborArray)?.elementToByteArray(0)
            requireNotNull(requiredSignerBytes) { "Invalid transaction! required signer bytes are missing!" }
            val requiredSignerStakeAddress =
                Bech32.encode(
                    if (isMainnet) {
                        "stake"
                    } else {
                        "stake_test"
                    },
                    byteArrayOf(challengeStakeAddressHeaderByte) + requiredSignerBytes
                )
            require(requiredSignerStakeAddress == challengeStakeAddress) {
                "Stake address mismatch!, challenge: $challengeStakeAddress, required signer: $requiredSignerStakeAddress"
            }

            // process the signatures
            val signatures = (tx.elementAt(1) as? CborMap)?.get(CborInteger.create(0)) as? CborArray
            requireNotNull(signatures) { "Invalid transaction! signatures are missing!" }
            var foundSignature = false
            var isVerified = false
            signatures.forEach {
                val pubKey = (it as CborArray).elementToByteArray(0)
                val signature = it.elementToByteArray(1)
                val pkh = Blake2b.hash224(pubKey)
                if (pkh.contentEquals(requiredSignerBytes)) {
                    // check our stake key's signature
                    foundSignature = true
                    val publicKey =
                        signingKey {
                            vkey = pubKey.toByteString()
                        }
                    isVerified = publicKey.verify(txId.hexToByteArray(), signature)
                    return@forEach
                }
            }
            require(foundSignature) { "Invalid transaction! required signer signature is missing!" }
            return verifySignDataResponse {
                verified = isVerified
                challenge = challengeString
            }
        } catch (e: Throwable) {
            Sentry.addBreadcrumb(request.toString(), "NewmChainService")
            log.error(e) { "verifySignTransaction error!" }
            return verifySignDataResponse {
                verified = false
                challenge = ""
                errorMessage = "verifySignTransaction error!: $e"
            }
        }
    }
}
