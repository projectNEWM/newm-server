package io.newm.server.features.walletconnection.model

import io.newm.chain.grpc.Utxo
import io.newm.server.ktx.cborHexToUtxos
import kotlinx.serialization.Serializable

@Serializable
data class GenerateChallengeRequest(
    val method: ChallengeMethod,
    val stakeAddress: String,
    // UTXOs and change address only needed if method = SignedTransaction
    val utxoCborHexList: List<String>? = null,
    val changeAddress: String? = null,
) {
    val utxos: List<Utxo> by lazy { utxoCborHexList.cborHexToUtxos() }
}
