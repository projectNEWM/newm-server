package io.newm.server.features.paypal.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PayPalCaptureOrderResponse(
    @SerialName("status")
    val status: String,
    @SerialName("purchase_units")
    val purchaseUnits: List<PurchaseUnit>
) {
    @Serializable
    data class PurchaseUnit(
        @SerialName("payments")
        val payments: Payments
    )

    @Serializable
    data class Payments(
        @SerialName("captures")
        val captures: List<Capture>
    )

    @Serializable
    data class Capture(
        @SerialName("invoice_id")
        val invoiceId: String,
        @SerialName("status")
        val status: String,
        @SerialName("amount")
        val amount: PayPalAmount
    )
}
