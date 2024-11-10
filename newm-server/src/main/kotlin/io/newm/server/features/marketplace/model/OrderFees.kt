package io.newm.server.features.marketplace.model

import kotlinx.serialization.Serializable

@Serializable
data class OrderFees(
    val serviceFeePercentage: Double,
    val profitAmountUsd: Double
)
