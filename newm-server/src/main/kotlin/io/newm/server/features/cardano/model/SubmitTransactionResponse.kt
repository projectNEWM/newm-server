package io.newm.server.features.cardano.model

import kotlinx.serialization.Serializable

@Serializable
data class SubmitTransactionResponse(
    val txId: String? = null,
    val result: String? = null,
)
