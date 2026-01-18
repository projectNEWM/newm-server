package io.newm.ardrive.turbo.upload.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateChunkedUploadResponse(
    val id: String,
    val min: Int,
    val max: Int,
    val chunkSize: Long,
)
