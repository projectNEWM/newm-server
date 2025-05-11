package io.newm.server.features.song.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MintPaymentOption(
    @SerialName("paymentType")
    val paymentType: PaymentType,
    @SerialName("cborHex")
    val cborHex: String,
    @SerialName("price")
    val price: String? = null,
    @SerialName("usdPrice")

)
