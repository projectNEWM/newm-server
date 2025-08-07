package io.newm.server.features.paypal.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PayPalCreateOrderRequest(
    @SerialName("intent")
    val intent: String,
    @SerialName("purchase_units")
    val purchaseUnits: List<PurchaseUnit>,
    @SerialName("payment_source")
    val paymentSource: PaymentSource,
) {
    @Serializable
    data class PurchaseUnit(
        @SerialName("invoice_id")
        val invoiceId: String,
        @SerialName("amount")
        val amount: PayPalAmount
    )

    @Serializable
    data class PaymentSource(
        @SerialName("paypal")
        val paypal: PayPal
    )

    @Serializable
    data class PayPal(
        @SerialName("experience_context")
        val experienceContext: ExperienceContext
    )

    @Serializable
    data class ExperienceContext(
        @SerialName("brand_name")
        val brandName: String,
        @SerialName("payment_method_preference")
        val paymentMethodPreference: String,
        @SerialName("shipping_preference")
        val shippingPreference: String,
        @SerialName("user_action")
        val userAction: String,
        @SerialName("return_url")
        val returnUrl: String,
        @SerialName("cancel_url")
        val cancelUrl: String
    )
}
