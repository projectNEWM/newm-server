package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CoverImage(
    @SerialName("url")
    val url: String,
    @SerialName("extension")
    val extension: String
)
