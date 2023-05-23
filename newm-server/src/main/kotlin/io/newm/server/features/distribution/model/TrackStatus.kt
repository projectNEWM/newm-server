package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TrackStatus(
    @SerialName("stereo")
    val stereo: String? = null,
    @SerialName("dolby")
    val dolby: String? = null,
    @SerialName("sony360")
    val sony360: Sony360? = null,
)
