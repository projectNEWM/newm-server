package io.newm.server.features.song.model

import io.newm.chain.grpc.Utxo
import io.newm.server.ktx.cborHexToUtxos
import kotlinx.serialization.Serializable

@Serializable
data class MintPaymentRequest(
    val changeAddress: String,
    val utxoCborHexList: List<String>? = null,
) {
    val utxos: List<Utxo> by lazy { utxoCborHexList.cborHexToUtxos() }
}
