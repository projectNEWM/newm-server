package io.newm.ardrive.turbo.upload.model

import io.newm.ardrive.turbo.model.TokenType
import kotlinx.serialization.Serializable

@Serializable
data class UploadServiceInfo(
    val version: String,
    val gateway: String,
    val freeUploadLimitBytes: Long,
    val addresses: Map<TokenType, String>,
)
