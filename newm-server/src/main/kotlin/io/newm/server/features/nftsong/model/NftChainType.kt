package io.newm.server.features.nftsong.model

import kotlinx.serialization.Serializable

@Serializable
enum class NftChainType {
    Cardano,
    Ethereum
}
