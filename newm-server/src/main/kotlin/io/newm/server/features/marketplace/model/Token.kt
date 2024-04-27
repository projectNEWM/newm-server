package io.newm.server.features.marketplace.model

import io.newm.chain.util.hexStringToAssetName

data class Token(
    val policyId: String,
    val assetName: String,
    val amount: Long
) {
    override fun toString(): String = "Token(policyId=$policyId,assetName=$assetName (${assetName.hexStringToAssetName()}),amount=$amount)"
}
