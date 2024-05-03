package io.newm.server.features.marketplace.model

import io.newm.chain.grpc.Utxo
import io.newm.server.ktx.cborHexToUtxos
import io.newm.shared.serialization.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class OrderTransactionRequest(
    @Serializable(with = UUIDSerializer::class)
    val orderId: UUID,
    val changeAddress: String,
    val utxoCborHexList: List<String>,
) {
    val utxos: List<Utxo> by lazy { utxoCborHexList.cborHexToUtxos() }
}
