package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PriceCode(
    @SerialName("album_price_id")
    val albumPriceId: Long,
    @SerialName("track_price_id")
    val trackPriceId: Long,
)
