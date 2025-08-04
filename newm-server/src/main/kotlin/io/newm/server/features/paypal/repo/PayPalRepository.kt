package io.newm.server.features.paypal.repo

import io.newm.server.features.paypal.model.MintingDistributionOrderRequest
import io.newm.server.features.paypal.model.MintingDistributionOrderResponse
import io.newm.server.typealiases.UserId

interface PayPalRepository {
    suspend fun createMintingDistributionOrder(
        requesterId: UserId,
        request: MintingDistributionOrderRequest
    ): MintingDistributionOrderResponse

    suspend fun captureMintingDistributionOrder(orderId: String)
}
