package io.newm.ardrive.turbo.payment.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class RawTokenPriceQuote(
    val winc: String,
    val fees: List<Adjustment> = emptyList(),
    @SerialName("actualPaymentAmount")
    val actualPaymentAmount: JsonPrimitive,
)
