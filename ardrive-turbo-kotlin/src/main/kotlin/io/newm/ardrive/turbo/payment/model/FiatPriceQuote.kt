package io.newm.ardrive.turbo.payment.model

import kotlinx.serialization.Serializable

@Serializable
data class FiatPriceQuote(
    val winc: String,
    val adjustments: List<Adjustment> = emptyList(),
    val fees: List<Adjustment> = emptyList(),
    val actualPaymentAmount: Double,
    val quotedPaymentAmount: Double,
)
