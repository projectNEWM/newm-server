package io.newm.server.features.marketplace.model

interface Transaction {
    val tokens: List<Token>
    val ownerAddress: String

    fun getToken(policyId: String): Token? = tokens.firstOrNull { it.policyId == policyId }

    fun getToken(
        policyId: String,
        assetName: String
    ): Token? = tokens.firstOrNull { it.policyId == policyId && it.assetName == assetName }
}
