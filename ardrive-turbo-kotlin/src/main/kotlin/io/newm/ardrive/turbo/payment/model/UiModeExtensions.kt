package io.newm.ardrive.turbo.payment.model

fun UiMode.toApiValue(): String =
    when (this) {
        UiMode.HOSTED -> "hosted"
        UiMode.EMBEDDED -> "embedded"
    }
