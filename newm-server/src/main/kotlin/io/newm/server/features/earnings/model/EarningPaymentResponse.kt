package io.newm.server.features.earnings.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EarningPaymentResponse(
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
    @SerialName("collabPriceAda")
    val collabPriceAda: String? = null,
    @SerialName("collabPriceUsd")
    val collabPriceUsd: String? = null,
    @SerialName("collabPerArtistPriceAda")
    val collabPerArtistPriceAda: String? = null,
    @SerialName("collabPerArtistPriceUsd")
    val collabPerArtistPriceUsd: String? = null,
    @SerialName("usdAdaExchangeRate")
    val usdAdaExchangeRate: String? = null,
)
