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
    val price: String,
    @SerialName("priceUsd")
    val priceUsd: String,
    @SerialName("dspPrice")
    val dspPrice: String,
    @SerialName("dspPriceUsd")
    val dspPriceUsd: String,
    @SerialName("mintPrice")
    val mintPrice: String,
    @SerialName("mintPriceUsd")
    val mintPriceUsd: String,
    @SerialName("collabPrice")
    val collabPrice: String,
    @SerialName("collabPriceUsd")
    val collabPriceUsd: String,
    @SerialName("collabPricePerArtist")
    val collabPricePerArtist: String,
    @SerialName("collabPricePerArtistUsd")
    val collabPricePerArtistUsd: String,
    @SerialName("usdToPaymentTypeExchangeRate")
    val usdToPaymentTypeExchangeRate: String,
) {
    // Constructor for USD-based payment types
    constructor(
        paymentType: PaymentType,
        priceUsd: String,
        dspPriceUsd: String,
        mintPriceUsd: String,
        collabPriceUsd: String,
        collabPricePerArtistUsd: String,
    ) : this(
        paymentType = paymentType,
        cborHex = "",
        price = priceUsd,
        priceUsd = priceUsd,
        dspPrice = dspPriceUsd,
        dspPriceUsd = dspPriceUsd,
        mintPrice = mintPriceUsd,
        mintPriceUsd = mintPriceUsd,
        collabPrice = collabPriceUsd,
        collabPriceUsd = collabPriceUsd,
        collabPricePerArtist = collabPricePerArtistUsd,
        collabPricePerArtistUsd = collabPricePerArtistUsd,
        usdToPaymentTypeExchangeRate = "1.000000"
    )
}
