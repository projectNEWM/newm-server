package io.newm.ardrive.turbo.payment.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class TopUpMethod {
    @SerialName("checkout-session")
    CHECKOUT_SESSION,

    @SerialName("payment-intent")
    PAYMENT_INTENT,
}
