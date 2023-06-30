package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PriceCodeList(
    @SerialName("album")
    val albumPrices: List<AlbumPrice>,
    @SerialName("track")
    val trackPrices: List<TrackPrice>
)
