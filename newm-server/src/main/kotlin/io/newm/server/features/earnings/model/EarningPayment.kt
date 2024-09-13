package io.newm.server.features.earnings.model

import io.newm.chain.grpc.Utxo
import io.newm.server.ktx.cborHexToUtxos
import kotlinx.serialization.Serializable

@Serializable
data class EarningPayment(
    val changeAddress: String,
    val utxoCborHexList: List<String>? = null,
) {
    val utxos: List<Utxo> by lazy { utxoCborHexList.cborHexToUtxos() }
}
