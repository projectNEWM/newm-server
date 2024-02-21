package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Track(
    @SerialName("track_id")
    val trackId: Long,
    @SerialName("artists")
    val artistIds: List<Long>,
    @SerialName("featured_artists")
    val featuredArtists: List<Long>?,
    @SerialName("preview")
    val preview: Preview?,
    @SerialName("participant")
    val participants: List<Participant>,
    @SerialName("instrumental")
    val instrumental: Boolean,
)
