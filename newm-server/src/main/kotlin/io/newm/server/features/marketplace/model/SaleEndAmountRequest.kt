package io.newm.server.features.marketplace.model

import io.newm.server.typealiases.SaleId
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class SaleEndAmountRequest(
    @Contextual
    val saleId: SaleId
)
