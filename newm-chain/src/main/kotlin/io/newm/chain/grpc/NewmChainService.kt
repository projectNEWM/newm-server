package io.newm.chain.grpc

import io.newm.chain.database.repository.LedgerRepository
import io.newm.chain.util.toHexString
import io.newm.kogmios.protocols.messages.SubmitFail
import io.newm.kogmios.protocols.messages.SubmitSuccess
import io.newm.objectpool.useInstance
import io.newm.server.di.inject
import org.koin.core.parameter.parametersOf
import org.slf4j.Logger

class NewmChainService : NewmChainGrpcKt.NewmChainCoroutineImplBase() {
    private val log: Logger by inject { parametersOf("NewmChainService") }
    private val ledgerRepository: LedgerRepository by inject()
    private val txSubmitClientPool: TxSubmitClientPool by inject()

    override suspend fun queryUtxos(request: QueryUtxosRequest): QueryUtxosResponse {
        val utxos = ledgerRepository.queryUtxos(request.address)

        return QueryUtxosResponse.newBuilder().apply {
            utxos.forEach { utxo ->
                addUtxos(
                    Utxo.newBuilder().apply {
                        hash = utxo.hash
                        ix = utxo.ix
                        lovelace = utxo.lovelace.toString()
                        utxo.datumHash?.let { datumHash = it }
                        utxo.datum?.let { datum = it }
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
    }

    override suspend fun submitTransaction(request: SubmitTransactionRequest): SubmitTransactionResponse {
        val cbor = request.cbor.toByteArray().toHexString()
        return txSubmitClientPool.useInstance { client ->
            val response = client.submit(cbor)
            when (response.result) {
                is SubmitSuccess -> {
                    // TODO: Add results of this successful tx to the Live UTxO state (needed for queryLiveUtxos)

                    SubmitTransactionResponse.newBuilder().apply {
                        result = "MsgAcceptTx"
                        txId = (response.result as SubmitSuccess).txId
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
}
