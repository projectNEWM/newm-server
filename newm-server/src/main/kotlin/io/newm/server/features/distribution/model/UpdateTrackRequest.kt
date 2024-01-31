package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateTrackRequest(
    @SerialName("uuid")
    val uuid: String,
    @SerialName("name")
    val name: String,
    @SerialName("stereo_isrc")
    val stereoIsrc: String? = null,
    @SerialName("iswc")
    val iswc: String? = null,
    @SerialName("genre")
    val genre: List<Long>? = null,
    @SerialName("language")
    val language: String? = null,
    // Explicit : 0-Clean, 1-Explicit, 2-Not Required
    @SerialName("explicit")
    val explicit: Int? = null,
    // 1 = Download, 2 = Streaming
    @SerialName("availability")
    val availability: List<Int>,
    @SerialName("artists")
    val artists: List<Long>? = null,
    @SerialName("featured_artists")
    val featuredArtists: List<Long>? = null,
    // false = Single, true = Album
    @SerialName("album_only")
    val albumOnly: Boolean = false,
    @SerialName("lyrics")
    val lyrics: String? = null,
    @SerialName("dolby_atmos_isrc")
    val dolbyAtmosIsrc: String? = null,
    @SerialName("sony_360ra_isrc")
    val sony360raIsrc: String? = null,
)
