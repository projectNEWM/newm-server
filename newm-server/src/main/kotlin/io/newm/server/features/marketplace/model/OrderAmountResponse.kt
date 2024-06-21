package io.newm.server.features.marketplace.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class OrderAmountResponse(
    @Contextual
    val orderId: UUID,
    val amountCborHex: String
)
