package io.newm.chain.grpc

import io.newm.chain.database.repository.LedgerRepository
import io.newm.server.di.inject

class NewmChainService : NewmChainGrpcKt.NewmChainCoroutineImplBase() {

    private val ledgerRepository: LedgerRepository by inject()

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
}
