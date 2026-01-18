package io.newm.ardrive.turbo.payment.model

import kotlinx.serialization.Serializable

@Serializable
data class TopUpQuote(
    val winc: String,
    val adjustments: List<Adjustment> = emptyList(),
    val fees: List<Adjustment> = emptyList(),
    val id: String,
    val url: String? = null,
    val clientSecret: String? = null,
    val paymentAmount: Double,
    val actualPaymentAmount: Double,
    val quotedPaymentAmount: Double,
)
