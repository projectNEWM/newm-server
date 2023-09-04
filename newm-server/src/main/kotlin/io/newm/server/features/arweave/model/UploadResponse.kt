package io.newm.server.features.arweave.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UploadResponse(
    @SerialName("id")
    val id: String,
    @SerialName("timestamp")
    val timestamp: Long,
)
