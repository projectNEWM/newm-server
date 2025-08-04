package io.newm.server.features.paypal.model

import kotlinx.serialization.Serializable

@Serializable
data class MintingDistributionOrderResponse(
    val orderId: String,
    val checkoutUrl: String
)
