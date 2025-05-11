package io.newm.server.features.song.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MintPaymentResponse(
    @SerialName("cborHex")
    @Deprecated("Use value in `mintPaymentOptions` instead")
    val cborHex: String,
    @SerialName("adaPrice")
    @Deprecated("Use value in `mintPaymentOptions` instead")
    val adaPrice: String? = null,
    @SerialName("usdPrice")
    @Deprecated("Use value in `mintPaymentOptions` instead")
    val usdPrice: String? = null,
    @SerialName("dspPriceAda")
    @Deprecated("Use value in `mintPaymentOptions` instead")
    val dspPriceAda: String? = null,
    @SerialName("dspPriceUsd")
    @Deprecated("Use value in `mintPaymentOptions` instead")
    val dspPriceUsd: String? = null,
    @SerialName("mintPriceAda")
    @Deprecated("Use value in `mintPaymentOptions` instead")
    val mintPriceAda: String? = null,
    @SerialName("mintPriceUsd")
    @Deprecated("Use value in `mintPaymentOptions` instead")
    val mintPriceUsd: String? = null,
    @SerialName("collabPriceAda")
    @Deprecated("Use value in `mintPaymentOptions` instead")
    val collabPriceAda: String? = null,
    @SerialName("collabPriceUsd")
    @Deprecated("Use value in `mintPaymentOptions` instead")
    val collabPriceUsd: String? = null,
    @SerialName("collabPerArtistPriceAda")
    @Deprecated("Use value in `mintPaymentOptions` instead")
    val collabPerArtistPriceAda: String? = null,
    @SerialName("collabPerArtistPriceUsd")
    @Deprecated("Use value in `mintPaymentOptions` instead")
    val collabPerArtistPriceUsd: String? = null,
    @SerialName("usdAdaExchangeRate")
    @Deprecated("Use value in `mintPaymentOptions` instead")
    val usdAdaExchangeRate: String? = null,
    @SerialName("mintPaymentOptions")
    val mintPaymentOptions: List<MintPaymentOption>? = null,
)
