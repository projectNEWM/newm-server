package io.newm.chain.grpc

import io.newm.chain.database.repository.LedgerRepository
import io.newm.chain.ledger.SubmittedTransactionCache
import io.newm.chain.util.toHexString
import io.newm.kogmios.protocols.messages.SubmitFail
import io.newm.kogmios.protocols.messages.SubmitSuccess
import io.newm.kogmios.protocols.model.Block
import io.newm.objectpool.useInstance
import io.newm.shared.koin.inject
import io.newm.txbuilder.ktx.cborHexToPlutusData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.slf4j.Logger

class NewmChainService : NewmChainGrpcKt.NewmChainCoroutineImplBase() {
    private val log: Logger by inject { parametersOf("NewmChainService") }
    private val ledgerRepository: LedgerRepository by inject()
    private val txSubmitClientPool: TxSubmitClientPool by inject()
    private val submittedTransactionCache: SubmittedTransactionCache by inject()
    private val blockFlow: MutableSharedFlow<Block> by inject(named("blockFlow"))

    override suspend fun queryUtxos(request: QueryUtxosRequest): QueryUtxosResponse =
        ledgerRepository.queryUtxos(request.address).toQueryUtxosResponse()

    override suspend fun queryLiveUtxos(request: QueryUtxosRequest): QueryUtxosResponse =
        ledgerRepository.queryLiveUtxos(request.address).toQueryUtxosResponse()

    private fun Set<io.newm.chain.model.Utxo>.toQueryUtxosResponse(): QueryUtxosResponse =
        QueryUtxosResponse.newBuilder().apply {
            this@toQueryUtxosResponse.forEach { utxo ->
                addUtxos(
                    Utxo.newBuilder().apply {
                        hash = utxo.hash
                        ix = utxo.ix
                        lovelace = utxo.lovelace.toString()
                        utxo.datumHash?.let { datumHash = it }
                        utxo.datum?.let {
                            datum = it.cborHexToPlutusData()
                        }
                        utxo.nativeAssets.forEach { nativeAsset ->
                            addNativeAssets(
                                NativeAsset.newBuilder().apply {
                                    policy = nativeAsset.policy
                                    name = nativeAsset.name
                                    amount = nativeAsset.amount.toString()
                                }
                            )
                        }
                    }
                )
            }
        }.build()

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

                    SubmitTransactionResponse.newBuilder().apply {
                        result = "MsgAcceptTx"
                        txId = transactionId
                    }.build()
                }

                is SubmitFail -> {
                    SubmitTransactionResponse.newBuilder().apply {
                        result = response.result.toString()
                    }.build()
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
}
