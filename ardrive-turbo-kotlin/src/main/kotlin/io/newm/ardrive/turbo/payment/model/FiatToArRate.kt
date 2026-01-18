package io.newm.ardrive.turbo.payment.model

import kotlinx.serialization.Serializable

@Serializable
data class FiatToArRate(
    val currency: Currency,
    val rate: Double,
)
