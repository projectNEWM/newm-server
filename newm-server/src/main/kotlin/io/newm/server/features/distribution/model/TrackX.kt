package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TrackX(
    @SerialName("album_track_id")
    val albumTrackId: Long,
    @SerialName("participant")
    val participant: List<ParticipantX>,
    @SerialName("iswc")
    val iswc: String,
    @SerialName("featured_artists")
    val featuredArtists: List<Artist>,
    @SerialName("artists")
    val artists: List<Artist>,
    @SerialName("isrc")
    val isrc: String,
    @SerialName("preview")
    val preview: Preview,
    @SerialName("outlets")
    val outlets: List<Long>,
    @SerialName("track_id")
    val trackId: Long,
    @SerialName("name")
    val name: String
)
