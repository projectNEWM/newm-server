package io.newm.server.features.cardano.model

import kotlinx.serialization.Serializable

@Serializable
data class SignedTransaction(
    val cborHex: String
)
