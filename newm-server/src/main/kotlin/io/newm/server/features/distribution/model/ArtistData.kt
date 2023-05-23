package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ArtistData(
    @SerialName("artist_name")
    val artistName: String,
    @SerialName("artist_id")
    val artistId: Long
)
