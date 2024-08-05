package io.newm.server.features.marketplace.model

import io.newm.server.typealiases.SaleId
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class OrderAmountRequest(
    @Contextual
    val saleId: SaleId,
    val bundleQuantity: Long,
    val incentiveAmount: Long?
)
