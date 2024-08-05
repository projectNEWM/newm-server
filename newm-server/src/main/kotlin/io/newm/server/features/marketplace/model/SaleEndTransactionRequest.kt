package io.newm.server.features.marketplace.model

import io.newm.chain.grpc.Utxo
import io.newm.server.ktx.cborHexToUtxos
import io.newm.server.typealiases.SaleId
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class SaleEndTransactionRequest(
    @Contextual
    val saleId: SaleId,
    val changeAddress: String,
    val utxoCborHexList: List<String>,
) {
    val utxos: List<Utxo> by lazy { utxoCborHexList.cborHexToUtxos() }
}
