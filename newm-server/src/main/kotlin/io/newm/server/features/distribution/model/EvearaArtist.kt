package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EvearaArtist(
    @SerialName("artist_id")
    val artistId: Long,
    @SerialName("is_active")
    val isActive: Int, // 1 == active, 0 == inactive
    @SerialName("featureArtist")
    val featureArtist: Int,
    @SerialName("outlets")
    val outlets: List<OutletProfile>,
    @SerialName("removable")
    val removable: Boolean,
    @SerialName("name")
    val name: String,
    @SerialName("tracks")
    val tracks: Int,
    @SerialName("releases")
    val releases: Int,
    @SerialName("country")
    val country: String,
)
