package io.newm.ardrive.turbo.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class TokenType {
    @SerialName("arweave")
    ARWEAVE,

    @SerialName("ario")
    ARIO,

    @SerialName("base-ario")
    BASE_ARIO,

    @SerialName("solana")
    SOLANA,

    @SerialName("ed25519")
    ED25519,

    @SerialName("ethereum")
    ETHEREUM,

    @SerialName("kyve")
    KYVE,

    @SerialName("matic")
    MATIC,

    @SerialName("pol")
    POL,

    @SerialName("base-eth")
    BASE_ETH,

    @SerialName("usdc")
    USDC,

    @SerialName("base-usdc")
    BASE_USDC,

    @SerialName("polygon-usdc")
    POLYGON_USDC,
}
