package io.newm.server.features.marketplace.model

import io.newm.chain.grpc.Utxo
import io.newm.server.ktx.cborHexToUtxos
import io.newm.server.typealiases.PendingSaleId
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class SaleStartTransactionRequest(
    @Contextual
    val saleId: PendingSaleId,
    val changeAddress: String,
    val email: String?, // Optional, to get notifications
    val utxoCborHexList: List<String>,
) {
    val utxos: List<Utxo> by lazy { utxoCborHexList.cborHexToUtxos() }
}
