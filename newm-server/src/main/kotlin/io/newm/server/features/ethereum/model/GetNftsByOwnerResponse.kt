package io.newm.server.features.ethereum.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetNftsByOwnerResponse(
    @SerialName("ownedNfts") val ownedNfts: List<EthereumNft>
)
