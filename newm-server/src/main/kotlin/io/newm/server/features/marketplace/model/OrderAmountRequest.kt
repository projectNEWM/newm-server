package io.newm.server.features.marketplace.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class OrderAmountRequest(
    @Contextual
    val saleId: UUID,
    val bundleQuantity: Long,
    val incentiveAmount: Long?
)
