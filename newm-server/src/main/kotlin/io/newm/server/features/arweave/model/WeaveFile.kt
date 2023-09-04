package io.newm.server.features.arweave.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeaveFile(
    @SerialName("url")
    val url: String,
    @SerialName("contentType")
    val contentType: String,
)
