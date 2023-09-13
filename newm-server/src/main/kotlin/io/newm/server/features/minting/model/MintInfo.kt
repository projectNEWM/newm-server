package io.newm.server.features.minting.model

/**
 * The result of a minting operation
 */
data class MintInfo(
    val transactionId: String,
    val policyId: String,
    val assetName: String,
)
