package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateArtistResponse(
    @SerialName("message")
    val message: String,
    @SerialName("success")
    val success: Boolean,
    @SerialName("data")
    val artistData: ArtistData? = null,
)
