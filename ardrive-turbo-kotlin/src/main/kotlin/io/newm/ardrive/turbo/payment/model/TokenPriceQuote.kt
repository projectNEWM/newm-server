package io.newm.ardrive.turbo.payment.model

import kotlinx.serialization.Serializable

@Serializable
data class TokenPriceQuote(
    val winc: String,
    val fees: List<Adjustment> = emptyList(),
    val actualTokenAmount: String,
    val equivalentWincTokenAmount: String,
)
