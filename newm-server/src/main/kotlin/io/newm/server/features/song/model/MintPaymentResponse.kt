package io.newm.server.features.song.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MintPaymentResponse(
    @SerialName("cborHex")
    val cborHex: String,
    @SerialName("adaPrice")
    val adaPrice: String? = null,
    @SerialName("usdPrice")
    val usdPrice: String? = null,
    @SerialName("dspPriceAda")
    val dspPriceAda: String? = null,
    @SerialName("dspPriceUsd")
    val dspPriceUsd: String? = null,
    @SerialName("mintPriceAda")
    val mintPriceAda: String? = null,
    @SerialName("mintPriceUsd")
    val mintPriceUsd: String? = null,
    @SerialName("sendTokenFeeAda")
    val sendTokenFeeAda: String? = null,
    @SerialName("sendTokenFeeUsd")
    val sendTokenFeeUsd: String? = null,
    @SerialName("usdAdaExchangeRate")
    val usdAdaExchangeRate: String? = null,
)
