package io.newm.ardrive.turbo.payment.model

import kotlinx.serialization.Serializable

@Serializable
data class SupportedCurrencies(
    val supportedCurrencies: List<Currency> = emptyList(),
    val limits: Map<Currency, CurrencyLimit> = emptyMap(),
)
