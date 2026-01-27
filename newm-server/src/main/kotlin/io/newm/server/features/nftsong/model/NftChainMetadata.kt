package io.newm.server.features.nftsong.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@JsonClassDiscriminator("chain")
sealed interface NftChainMetadata {
    @Serializable
    @SerialName("Cardano")
    data class Cardano(
        val fingerprint: String,
        val policyId: String,
        val assetName: String,
        val isStreamToken: Boolean
    ) : NftChainMetadata

    @Serializable
    @SerialName("Ethereum")
    data class Ethereum(
        val contractAddress: String,
        val tokenType: String,
        val tokenId: String
    ) : NftChainMetadata
}
