package io.newm.server.features.marketplace.model

import io.newm.server.typealiases.PendingSaleId
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class SaleStartAmountResponse(
    @Contextual
    val saleId: PendingSaleId,
    val amountCborHex: String
)
