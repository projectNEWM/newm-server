package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FeaturedArtist(
    @SerialName("artist_id")
    val artistId: Long,
    @SerialName("name")
    val name: String
)
