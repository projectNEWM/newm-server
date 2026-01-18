package io.newm.ardrive.turbo.payment.model

fun Currency.toApiValue(): String =
    when (this) {
        Currency.USD -> "usd"
        Currency.EUR -> "eur"
        Currency.GBP -> "gbp"
        Currency.CAD -> "cad"
        Currency.AUD -> "aud"
        Currency.JPY -> "jpy"
        Currency.INR -> "inr"
        Currency.SGD -> "sgd"
        Currency.HKD -> "hkd"
        Currency.BRL -> "brl"
    }
