package io.newm.server.features.paypal.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class PayPalAmount(
    @SerialName("currency_code")
    val currencyCode: String,
    @Contextual @SerialName("value")
    val value: BigDecimal,
    @SerialName("breakdown")
    val breakdown: Map<String, PayPalAmount>? = null
)
