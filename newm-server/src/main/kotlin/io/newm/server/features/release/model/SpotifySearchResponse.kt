package io.newm.server.features.release.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SpotifySearchResponse(
    @SerialName("tracks") val tracks: Tracks
)
