package io.newm.server.features.marketplace.model

data class QueueTransaction(
    override val tokens: List<Token>,
    override val ownerAddress: String,
    val numberOfBundles: Long,
    val incentive: Token,
    val pointerAssetName: String
) : Transaction
