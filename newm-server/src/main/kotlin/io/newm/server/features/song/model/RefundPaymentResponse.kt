package io.newm.server.features.song.model

import kotlinx.serialization.Serializable

@Serializable
data class RefundPaymentResponse(
    val transactionId: String,
    val message: String,
)
