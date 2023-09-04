package io.newm.server.features.arweave.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeaveResponseItem(
    @SerialName("url")
    val url: String,
    @SerialName("contentType")
    val contentType: String,
    @SerialName("id")
    val id: String? = null,
    @SerialName("error")
    val error: Map<String, @Contextual Any>? = null,
)
