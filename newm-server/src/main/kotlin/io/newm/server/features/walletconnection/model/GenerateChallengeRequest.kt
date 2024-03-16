package io.newm.server.features.walletconnection.model

import kotlinx.serialization.Serializable

@Serializable
data class GenerateChallengeRequest(
    val method: ChallengeMethod,
    val stakeAddress: String,
    // UTXOs only needed if method = SignedTransaction
    val utxos: List<String>? = null
)
