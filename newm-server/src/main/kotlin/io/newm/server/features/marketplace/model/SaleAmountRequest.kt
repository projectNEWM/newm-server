package io.newm.server.features.marketplace.model

import kotlinx.serialization.Serializable

@Serializable
data class SaleAmountRequest(
    val ownerAddress: String,
    val bundlePolicyId: String,
    val bundleAssetName: String,
    val bundleAmount: Long,
    val costPolicyId: String?, // Optional: if missing defaults to NEWM token
    val costAssetName: String?, // Optional: if missing defaults to NEWM token
    val costAmount: Long,
    val totalBundleQuantity: Long
)
