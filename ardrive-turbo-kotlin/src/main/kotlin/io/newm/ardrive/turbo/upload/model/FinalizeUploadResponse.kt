package io.newm.ardrive.turbo.upload.model

import kotlinx.serialization.Serializable

@Serializable
data class FinalizeUploadResponse(
    val status: String? = null,
)
