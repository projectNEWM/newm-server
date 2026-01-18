package io.newm.ardrive.turbo.payment.model

import kotlinx.serialization.Serializable

@Serializable
data class RatesResponse(
    val winc: String,
    val adjustments: List<Adjustment> = emptyList(),
    val fees: List<Adjustment> = emptyList(),
    val fiat: Map<Currency, Double> = emptyMap(),
)
