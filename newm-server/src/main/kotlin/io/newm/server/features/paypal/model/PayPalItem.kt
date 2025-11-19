package io.newm.server.features.paypal.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class PayPalItem(
    @SerialName("name")
    val name: String,
    @SerialName("quantity")
    val quantity: String,
    @SerialName("unit_amount")
    val unitAmount: PayPalAmount
) {
    constructor(
        name: String,
        unitPriceUsd: BigDecimal
    ) : this(
        name = name,
        quantity = "1",
        unitAmount = PayPalAmount(
            currencyCode = "USD",
            value = unitPriceUsd
        )
    )
}
