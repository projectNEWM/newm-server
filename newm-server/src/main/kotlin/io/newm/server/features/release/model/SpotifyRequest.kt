package io.newm.server.features.release.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SpotifyRequest(
    @SerialName("uris")
    val uris: Array<String>,
    @SerialName("position")
    val position: Int,
)