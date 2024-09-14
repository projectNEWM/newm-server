package io.newm.server.features.earnings.model

import io.newm.chain.grpc.Utxo
import io.newm.server.ktx.cborHexToUtxos
import kotlinx.serialization.Serializable

@Serializable
data class ClaimOrderRequest(
    val walletAddress: String,
    val changeAddress: String,
    val utxoCborHexList: List<String>,
) {
    val utxos: List<Utxo> by lazy { utxoCborHexList.cborHexToUtxos() }
}
