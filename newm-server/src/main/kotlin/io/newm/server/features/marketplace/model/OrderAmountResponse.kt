package io.newm.server.features.marketplace.model

import io.newm.shared.serialization.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class OrderAmountResponse(
    @Serializable(with = UUIDSerializer::class)
    val orderId: UUID,
    val amountCborHex: String
)
