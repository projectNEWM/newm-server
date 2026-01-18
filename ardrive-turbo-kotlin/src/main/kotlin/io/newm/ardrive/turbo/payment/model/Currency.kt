package io.newm.ardrive.turbo.payment.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Currency {
    @SerialName("usd")
    USD,

    @SerialName("eur")
    EUR,

    @SerialName("gbp")
    GBP,

    @SerialName("cad")
    CAD,

    @SerialName("aud")
    AUD,

    @SerialName("jpy")
    JPY,

    @SerialName("inr")
    INR,

    @SerialName("sgd")
    SGD,

    @SerialName("hkd")
    HKD,

    @SerialName("brl")
    BRL,
}
