package io.newm.ardrive.turbo.upload.model

import kotlinx.serialization.Serializable

@Serializable
data class UploadChunkResponse(
    val status: String? = null,
)
