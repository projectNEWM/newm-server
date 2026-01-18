package io.newm.ardrive.turbo.payment.model

import io.newm.ardrive.turbo.model.TokenType
import kotlinx.serialization.Serializable

@Serializable
data class TokenPriceForBytes(
    val tokenPrice: String,
    val byteCount: Long,
    val token: TokenType,
)
