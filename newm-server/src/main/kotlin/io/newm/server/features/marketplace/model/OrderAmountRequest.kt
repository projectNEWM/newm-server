package io.newm.server.features.marketplace.model

import io.newm.shared.serialization.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class OrderAmountRequest(
    @Serializable(with = UUIDSerializer::class)
    val saleId: UUID,
    val bundleQuantity: Long,
    val incentiveAmount: Long?
)
