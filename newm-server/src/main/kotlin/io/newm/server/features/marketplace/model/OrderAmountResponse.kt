package io.newm.server.features.marketplace.model

import io.newm.server.typealiases.PendingOrderId
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class OrderAmountResponse(
    @Contextual
    val orderId: PendingOrderId,
    val amountCborHex: String
)
