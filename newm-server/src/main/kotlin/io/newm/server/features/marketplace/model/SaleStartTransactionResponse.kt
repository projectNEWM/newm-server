package io.newm.server.features.marketplace.model

import kotlinx.serialization.Serializable

@Serializable
data class SaleStartTransactionResponse(
    val txCborHex: String
)
