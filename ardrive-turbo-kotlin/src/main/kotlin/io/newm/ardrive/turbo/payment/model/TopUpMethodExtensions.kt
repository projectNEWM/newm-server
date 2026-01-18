package io.newm.ardrive.turbo.payment.model

fun TopUpMethod.toApiValue(): String =
    when (this) {
        TopUpMethod.CHECKOUT_SESSION -> "checkout-session"
        TopUpMethod.PAYMENT_INTENT -> "payment-intent"
    }
