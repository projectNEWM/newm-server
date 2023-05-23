package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TrackData(
    @SerialName("language")
    val language: String,
    @SerialName("featured_artists")
    val featuredArtists: List<FeaturedArtist>? = null,
    @SerialName("album_only")
    val albumOnly: Boolean? = null,
    @SerialName("sony_360ra_isrc")
    val sony360raIsrc: String? = null,
    @SerialName("artists")
    val artists: List<Artist>,
    @SerialName("stereo_isrc")
    val stereoIsrc: String,
    @SerialName("genres")
    val genres: List<Genre>? = null,
    @SerialName("extention")
    val extention: String,
    @SerialName("name")
    val name: String,
    @SerialName("track_id")
    val trackId: Long,
    @SerialName("track_url")
    val trackUrl: String,
    @SerialName("iswc")
    val iswc: String? = null,
    @SerialName("availability")
    val availability: List<Availability>? = null,
    @SerialName("dolby_atmos_isrc")
    val dolbyAtmosIsrc: String? = null,
    @SerialName("removable")
    val removable: Boolean,
    @SerialName("explicit")
    val explicit: String,
)
