package io.newm.server.features.marketplace.model

import io.newm.shared.ktx.orZero

data class SaleTransaction(
    override val tokens: List<Token>,
    override val ownerAddress: String,
    val bundle: Token,
    val cost: Token,
    val maxBundleSize: Long
) : Transaction {
    val bundleQuantity: Long by lazy { getToken(bundle.policyId, bundle.assetName)?.amount.orZero() / bundle.amount }
}
