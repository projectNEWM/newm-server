package io.newm.chain.grpc

import com.google.protobuf.kotlin.toByteString
import io.newm.chain.cardano.address.Address
import io.newm.chain.cardano.address.AddressCredential
import io.newm.chain.cardano.address.BIP32PublicKey
import io.newm.chain.cardano.getCurrentEpoch
import io.newm.chain.cardano.toLedgerAssetMetadataItem
import io.newm.chain.config.Config
import io.newm.chain.database.repository.LedgerRepository
import io.newm.chain.ledger.SubmittedTransactionCache
import io.newm.chain.model.toNativeAssetMap
import io.newm.chain.util.Constants.ROLE_CHANGE
import io.newm.chain.util.Constants.ROLE_PAYMENT
import io.newm.chain.util.Constants.ROLE_STAKING
import io.newm.chain.util.hexToByteArray
import io.newm.chain.util.toCreatedUtxoMap
import io.newm.chain.util.toHexString
import io.newm.kogmios.StateQueryClient
import io.newm.kogmios.protocols.messages.EvaluationResult
import io.newm.kogmios.protocols.messages.SubmitFail
import io.newm.kogmios.protocols.messages.SubmitSuccess
import io.newm.kogmios.protocols.model.Block
import io.newm.kogmios.protocols.model.QueryCurrentProtocolBabbageParametersResult
import io.newm.objectpool.useInstance
import io.newm.shared.koin.inject
import io.newm.shared.ktx.info
import io.newm.shared.ktx.warn
import io.newm.txbuilder.TransactionBuilder
import io.newm.txbuilder.ktx.cborHexToPlutusData
import io.newm.txbuilder.ktx.toNativeAssetMap
import io.newm.txbuilder.ktx.withMinUtxo
import io.sentry.Sentry
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
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.slf4j.Logger
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class NewmChainService : NewmChainGrpcKt.NewmChainCoroutineImplBase() {
    private val log: Logger by inject { parametersOf("NewmChainService") }
    private val ledgerRepository: LedgerRepository by inject()
    private val txSubmitClientPool: TxSubmitClientPool by inject()
    private val submittedTransactionCache: SubmittedTransactionCache by inject()
    private val confirmedBlockFlow: MutableSharedFlow<Block> by inject(named("confirmedBlockFlow"))

    override suspend fun queryUtxos(request: QueryUtxosRequest): QueryUtxosResponse =
        ledgerRepository.queryUtxos(request.address).toQueryUtxosResponse()

    override suspend fun queryLiveUtxos(request: QueryUtxosRequest): QueryUtxosResponse =
        ledgerRepository.queryLiveUtxos(request.address).toQueryUtxosResponse()

    override suspend fun queryUtxosByOutputRef(request: QueryUtxosOutputRefRequest): QueryUtxosResponse {
        return ledgerRepository.queryUtxosByOutputRef(request.hash, request.ix.toInt()).toQueryUtxosResponse()
    }

    override suspend fun queryPublicKeyHashByOutputRef(request: QueryUtxosOutputRefRequest): QueryPublicKeyHashResponse {
        return ledgerRepository.queryPublicKeyHashByOutputRef(request.hash, request.ix.toInt())?.let {
            queryPublicKeyHashResponse {
                publicKeyHash = it
            }
        } ?: queryPublicKeyHashResponse { }
    }

    override suspend fun queryUtxosByStakeAddress(request: QueryUtxosRequest): QueryUtxosResponse {
        return ledgerRepository.queryUtxosByStakeAddress(request.address).toQueryUtxosResponse()
    }

    private fun Set<io.newm.chain.model.Utxo>.toQueryUtxosResponse(): QueryUtxosResponse =
        queryUtxosResponse {
            utxos.addAll(
                this@toQueryUtxosResponse.map { utxo ->
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
                }
            )
        }

    override suspend fun queryDatumByHash(request: QueryDatumByHashRequest): QueryDatumByHashResponse {
        return queryDatumByHashResponse {
            ledgerRepository.queryDatumByHash(request.datumHash)?.cborHexToPlutusData()?.let {
                datum = it
            }
        }
    }

    override suspend fun queryCurrentEpoch(request: QueryCurrentEpochRequest): QueryCurrentEpochResponse {
        return queryCurrentEpochResponse {
            epoch = getCurrentEpoch()
        }
    }

    override suspend fun queryTransactionConfirmationCount(request: QueryTransactionConfirmationCountRequest): QueryTransactionConfirmationCountResponse {
        return queryTransactionConfirmationCountResponse {
            txIdToConfirmationCount.putAll(
                ledgerRepository.queryTransactionConfirmationCounts(request.txIdsList)
            )
        }
    }

    override suspend fun submitTransaction(request: SubmitTransactionRequest): SubmitTransactionResponse {
        return try {
            val cbor = request.cbor.toByteArray()
            txSubmitClientPool.useInstance { client ->
                val response = client.submit(cbor.toHexString())
                when (response.result) {
                    is SubmitSuccess -> {
                        val transactionId = (response.result as SubmitSuccess).txId
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

                    is SubmitFail -> {
                        submitTransactionResponse {
                            result = response.result.toString()
                        }.also {
                            log.warn {
                                "submitTransaction(cbor: ${
                                    request.cbor.toByteArray().toHexString()
                                }) FAILED. result: ${it.result}"
                            }
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            Sentry.addBreadcrumb(request.toString(), "NewmChainService")
            log.error("submitTransaction(${request.cbor.toByteArray().toHexString()}) failed.", e)
            submitTransactionResponse {
                result =
                    "submitTransaction(${request.cbor.toByteArray().toHexString()}) failed. Exception: ${e.message}"
            }
        }
    }

    override fun monitorAddress(request: MonitorAddressRequest): Flow<MonitorAddressResponse> {
        log.warn("monitorAddress($request) started.")
        val responseFlow = MutableSharedFlow<MonitorAddressResponse>(replay = 1)
        val messageHandlerJob: Job = CoroutineScope(context).launch {
            try {
                var startAfterTxId: String? = if (request.hasStartAfterTxId()) {
                    request.startAfterTxId.trim()
                } else {
                    null
                }
                val limit = 1000
                var offset = 0L
                var nextStartAfterTxId: String? = startAfterTxId
                while (true) {
                    while (true) {
                        val monitorAddressResponseList = ledgerRepository.queryAddressTxLogsAfter(
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
                    log.error(e.message, e)
                }
            }
        }

        messageHandlerJob.invokeOnCompletion {
            log.warn("invokeOnCompletion: ${it?.message}")
        }
        return responseFlow
    }

    override suspend fun isMainnet(request: IsMainnetRequest): IsMainnetResponse {
        return isMainnetResponse {
            isMainnet = Config.isMainnet
        }
    }

    override suspend fun monitorPaymentAddress(request: MonitorPaymentAddressRequest): MonitorPaymentAddressResponse {
        try {
            val requestNativeAssetMap = request.nativeAssetsList.map {
                io.newm.chain.model.NativeAsset(
                    policy = it.policy,
                    name = it.name,
                    amount = it.amount.toBigInteger()
                )
            }.toNativeAssetMap()

            return withTimeoutOrNull(request.timeoutMs) {
                // Check utxos immediately to see if payment has already arrived
                val liveUtxosResponse = queryLiveUtxos(queryUtxosRequest { address = request.address })
                val existingUtxo = liveUtxosResponse.utxosList.firstOrNull { utxo ->
                    (utxo.lovelace == request.lovelace) && // lovelace matches
                        (requestNativeAssetMap == utxo.nativeAssetsList.toNativeAssetMap()) // nativeAssets match exactly
                }
                if (existingUtxo != null) {
                    monitorPaymentAddressResponse {
                        success = true
                        message = "Payment Received"
                    }
                } else {
                    // Wait for payment to arrive
                    val matchingUtxo = confirmedBlockFlow.mapNotNull { block ->
                        block.toCreatedUtxoMap().values.flatten().firstOrNull { createdUtxo ->
                            (createdUtxo.address == request.address) && // address matches
                                (createdUtxo.lovelace == request.lovelace.toBigInteger()) && // lovelace matches
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
            log.error("monitorPaymentAddress error!", e)
            throw e
        }
    }

    override suspend fun transactionBuilder(request: TransactionBuilderRequest): TransactionBuilderResponse {
        return try {
            txSubmitClientPool.useInstance { txSubmitClient ->
                val stateQueryClient = txSubmitClient as StateQueryClient
                val protocolParams =
                    stateQueryClient.currentProtocolParameters().result as QueryCurrentProtocolBabbageParametersResult
                val calculateTxExecutionUnits: suspend (ByteArray) -> EvaluationResult = { cborBytes ->
                    val evaluateResponse = txSubmitClient.evaluate(cborBytes.toHexString())
                    if (evaluateResponse.result !is EvaluationResult) {
                        log.warn { "evaluate failed, cbor: ${cborBytes.toHexString()}" }
                        throw IllegalStateException(evaluateResponse.result.toString())
                    }
                    (evaluateResponse.result as EvaluationResult)
                }

                val updatedRequest =
                    if (request.signaturesCount == 0 && request.signingKeysCount == 0 && request.requiredSignersCount == 0) {
                        // Calculate the number of different payment keys associated with all input utxos
                        val requiredSigners =
                            (request.sourceUtxosList + request.collateralUtxosList).mapNotNull { utxo ->
                                ledgerRepository.queryPublicKeyHashByOutputRef(utxo.hash, utxo.ix.toInt())
                            }.distinct().map {
                                it.hexToByteArray().toByteString()
                            }
                        request.toBuilder()
                            .addAllRequiredSigners(requiredSigners)
                            .build()
                    } else {
                        request
                    }

                val (txId, cborBytes) = TransactionBuilder.transactionBuilder(
                    protocolParams,
                    calculateTxExecutionUnits
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
            log.error("TransactionBuilder error!", e)
            transactionBuilderResponse {
                transactionId = ""
                transactionCbor = ByteArray(0).toByteString()
                errorMessage = e.message ?: "TransactionBuilder error!: $e"
            }
        }
    }

    override fun monitorNativeAssets(request: MonitorNativeAssetsRequest): Flow<MonitorNativeAssetsResponse> {
        log.info { "monitorNativeAssets request: $request" }
        val responseFlow = MutableSharedFlow<MonitorNativeAssetsResponse>(replay = 1)
        val messageHandlerJob: Job = CoroutineScope(context).launch {
            try {
                var startAfterId: Long? = if (request.hasStartAfterId()) {
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
                                    MonitorNativeAssetsResponse.parseFrom(nativeAssetLog.second)
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
                    log.error(e.message, e)
                }
            }
        }

        messageHandlerJob.invokeOnCompletion {
            log.warn("invokeOnCompletion: ${it?.message}")
        }
        return responseFlow
    }

    override suspend fun calculateMinUtxoForOutput(request: OutputUtxo): OutputUtxo {
        try {
            return txSubmitClientPool.useInstance { txSubmitClient ->
                val stateQueryClient = txSubmitClient as StateQueryClient
                val protocolParams =
                    stateQueryClient.currentProtocolParameters().result as QueryCurrentProtocolBabbageParametersResult
                request.withMinUtxo(protocolParams.coinsPerUtxoByte.toLong())
            }
        } catch (e: Throwable) {
            Sentry.addBreadcrumb(request.toString(), "NewmChainService")
            log.error("calculateMinUtxoForOutput error!", e)
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
            log.error("snapshotNativeAssets error!", e)
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
                this.stakeAddress = address {
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
            log.error("deriveWalletAddresses error!", e)
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
            val paymentAddresses = (indexStart..indexStart + 39u).map { index ->
                val paymentPk = rolePk.derive(index)
                val paymentCredential = AddressCredential.fromKey(paymentPk)
                stakeCredential?.let {
                    Address.fromPaymentStakeAddressCredentialsKeyKey(paymentCredential, it, Config.isMainnet).address
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

            val addressUtxosList = allUsedAddresses.mapNotNull { address ->
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
            log.error("queryWalletControlledUtxos error!", e)
            throw e
        }
    }

    override suspend fun queryUtxoByNativeAsset(request: QueryByNativeAssetRequest): Utxo {
        try {
            return ledgerRepository.queryUtxoByNativeAsset(request.name, request.policy)?.let { utxo ->
                utxo {
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
        } catch (e: Throwable) {
            Sentry.addBreadcrumb(request.toString(), "NewmChainService")
            log.error("queryUtxoByNativeAsset error!", e)
            throw e
        }
    }

    private val mutexMap = ConcurrentHashMap<String, Mutex>()
    private val mutexOwnerMap = ConcurrentHashMap<String, UUID>()
    private val mutexExpiryJobMap = ConcurrentHashMap<String, Job>()

    override suspend fun acquireMutex(request: AcquireMutexRequest): MutexResponse {
        try {
            val mutex = mutexMap.getOrPut(request.mutexName) { Mutex() }
            val locked = withTimeoutOrNull(request.acquireWaitTimeoutMs) {
                val owner = mutexOwnerMap.getOrPut(request.mutexName) { UUID.randomUUID() }
                mutex.lock(owner)
                true
            } ?: false

            return if (locked) {
                mutexExpiryJobMap[request.mutexName] = CoroutineScope(context).launch {
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
            log.error("acquireMutex error!", e)
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
            log.error("releaseMutex error!", e)
            throw e
        }
    }

    override suspend fun ping(request: PingRequest): PongResponse {
        return pongResponse {
            message = request.message
        }
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
            log.error("queryLedgerAssetMetadataListByNativeAsset error!", e)
            throw e
        }
    }
}
