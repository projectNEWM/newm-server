package io.newm.ardrive.turbo.payment.model

import kotlinx.serialization.Serializable

@Serializable
data class CurrencyLimit(
    val minimumPaymentAmount: Double,
    val maximumPaymentAmount: Double,
    val suggestedPaymentAmounts: List<Double> = emptyList(),
    val zeroDecimalCurrency: Boolean,
)
