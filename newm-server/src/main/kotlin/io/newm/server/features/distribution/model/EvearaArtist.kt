package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EvearaArtist(
    @SerialName("artist_id")
    val artistId: Long,
    // 1 == active, 0 == inactive
    @SerialName("is_active")
    val isActive: Int,
    @SerialName("featureArtist")
    val featureArtist: Int? = null,
    @SerialName("outlets")
    val outlets: List<OutletProfile>,
    @SerialName("removable")
    val removable: Boolean,
    @SerialName("name")
    val name: String,
    @SerialName("tracks")
    val tracks: Int? = null,
    @SerialName("releases")
    val releases: Int? = null,
    @SerialName("country")
    val country: String,
)
