package io.newm.server.features.cardano.model

import kotlinx.serialization.Serializable

@Serializable
data class SubmitTxRequest(
    val cborHex: String
)
