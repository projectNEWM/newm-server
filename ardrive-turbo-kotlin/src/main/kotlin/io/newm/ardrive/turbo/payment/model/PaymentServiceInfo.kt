package io.newm.ardrive.turbo.payment.model

import io.newm.ardrive.turbo.model.TokenType
import kotlinx.serialization.Serializable

@Serializable
data class PaymentServiceInfo(
    val version: String,
    val gateway: String,
    val addresses: Map<TokenType, String>,
)
