package io.newm.server.features.marketplace.model

import io.newm.chain.grpc.Utxo
import io.newm.server.ktx.cborHexToUtxos
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class SaleEndTransactionRequest(
    @Contextual
    val saleId: UUID,
    val changeAddress: String,
    val utxoCborHexList: List<String>,
) {
    val utxos: List<Utxo> by lazy { utxoCborHexList.cborHexToUtxos() }
}
