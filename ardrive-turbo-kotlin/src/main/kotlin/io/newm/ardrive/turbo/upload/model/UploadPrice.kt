package io.newm.ardrive.turbo.upload.model

import io.newm.ardrive.turbo.model.TokenType
import kotlinx.serialization.Serializable

@Serializable
data class UploadPrice(
    val tokenPrice: String,
    val byteCount: Long,
    val token: TokenType,
)
