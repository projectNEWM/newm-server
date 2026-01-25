package io.newm.server.features.nftsong.model

import kotlinx.serialization.Serializable

@Serializable
sealed interface NftChainMetadata {
    @Serializable
    data class Cardano(
        val fingerprint: String,
        val policyId: String,
        val assetName: String,
        val isStreamToken: Boolean
    ) : NftChainMetadata

    @Serializable
    data class Ethereum(
        val contractAddress: String,
        val tokenType: String,
        val tokenId: String,
    ) : NftChainMetadata
}
