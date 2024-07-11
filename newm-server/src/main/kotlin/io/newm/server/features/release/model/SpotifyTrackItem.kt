package io.newm.server.features.release.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SpotifyTrackItem(
    @SerialName("id") val id: String,
    @SerialName("track_number") val trackNumber: Int,
    @SerialName("uri") val uri: String
)
