package io.newm.chain.grpc

import com.google.protobuf.kotlin.toByteString
import io.newm.chain.cardano.getCurrentEpoch
import io.newm.chain.config.Config
import io.newm.chain.database.repository.LedgerRepository
import io.newm.chain.ledger.SubmittedTransactionCache
import io.newm.chain.model.toNativeAssetMap
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
import io.newm.txbuilder.TransactionBuilder
import io.newm.txbuilder.ktx.cborHexToPlutusData
import io.newm.txbuilder.ktx.toNativeAssetMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.slf4j.Logger

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

    private fun Set<io.newm.chain.model.Utxo>.toQueryUtxosResponse(): QueryUtxosResponse =
        queryUtxosResponse {
            utxos.addAll(
                this@toQueryUtxosResponse.map { utxo ->
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
                }
            )
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
        val cbor = request.cbor.toByteArray()
        return txSubmitClientPool.useInstance { client ->
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
                    }
                }

                is SubmitFail -> {
                    submitTransactionResponse {
                        result = response.result.toString()
                    }
                }
            }
        }
    }

    override fun monitorAddress(request: MonitorAddressRequest): Flow<MonitorAddressResponse> {
        log.warn("monitorAddress($request) started.")
        val responseFlow = MutableSharedFlow<MonitorAddressResponse>(replay = 1)
        val messageHandlerJob: Job = CoroutineScope(context).launch {
            try {
                var startAfterTxId: String? = request.startAfterTxId
                while (true) {
                    val monitorAddressResponseList = ledgerRepository.queryAddressTxLogsAfter(
                        request.address,
                        startAfterTxId,
                    ).map {
                        MonitorAddressResponse.parseFrom(it)
                    }

                    monitorAddressResponseList.forEach { monitorAddressResponse ->
                        responseFlow.emit(monitorAddressResponse)
                    }

                    if (monitorAddressResponseList.isNotEmpty()) {
                        startAfterTxId = monitorAddressResponseList.last().txId
                    }
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
        val requestNativeAssetMap = request.nativeAssetsList.map {
            io.newm.chain.model.NativeAsset(
                policy = it.policy,
                name = it.name,
                amount = it.amount.toBigInteger()
            )
        }.toNativeAssetMap()

        return withTimeoutOrNull(request.timeoutMs) {
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
        } ?: monitorPaymentAddressResponse {
            success = false
            message = "Timeout waiting for payment to arrive!"
        }
    }

    override suspend fun transactionBuilder(request: TransactionBuilderRequest): TransactionBuilderResponse {
        try {
            return txSubmitClientPool.useInstance { txSubmitClient ->
                val stateQueryClient = txSubmitClient as StateQueryClient
                val protocolParams =
                    stateQueryClient.currentProtocolParameters().result as QueryCurrentProtocolBabbageParametersResult
                val calculateTxExecutionUnits: suspend (ByteArray) -> EvaluationResult = { cborBytes ->
                    val evaluateResponse = txSubmitClient.evaluate(cborBytes.toHexString())
                    if (evaluateResponse.result !is EvaluationResult) {
                        throw IllegalStateException(evaluateResponse.result.toString())
                    }
                    (evaluateResponse.result as EvaluationResult)
                }

                if (request.signaturesCount == 0 && request.signingKeysCount == 0 && request.requiredSignersCount == 0) {
                    // Calculate the number of different payment keys associated with all input utxos
                    (request.sourceUtxosList + request.collateralUtxosList).mapNotNull { utxo ->
                        ledgerRepository.queryPublicKeyHashByOutputRef(utxo.hash, utxo.ix.toInt())
                    }.distinct().forEach {
                        request.requiredSignersList.add(it.hexToByteArray().toByteString())
                    }
                }

                val (txId, cborBytes) = TransactionBuilder.transactionBuilder(
                    protocolParams,
                    calculateTxExecutionUnits
                ) {
                    loadFrom(request)
                }
                transactionBuilderResponse {
                    transactionId = txId
                    transactionCbor = cborBytes.toByteString()
                }
            }
        } catch (e: Throwable) {
            log.error("TransactionBuilder error!", e)
            throw e
        }
    }

    override fun monitorNativeAssets(request: MonitorNativeAssetsRequest): Flow<MonitorNativeAssetsResponse> {
        val responseFlow = MutableSharedFlow<MonitorNativeAssetsResponse>(replay = 1)
        val messageHandlerJob: Job = CoroutineScope(context).launch {
            try {
                var startAfterId: Long? = if (request.hasStartAfterId()) {
                    request.startAfterId
                } else {
                    null
                }
                while (true) {
                    val nativeAssetLogList = ledgerRepository.queryNativeAssetLogsAfter(startAfterId)

                    nativeAssetLogList.forEach { nativeAssetLog ->
                        responseFlow.emit(
                            MonitorNativeAssetsResponse.parseFrom(nativeAssetLog.second)
                                .toBuilder()
                                .setId(nativeAssetLog.first)
                                .build()
                        )
                    }

                    if (nativeAssetLogList.isNotEmpty()) {
                        startAfterId = nativeAssetLogList.last().first
                    }
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
}
