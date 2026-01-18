package io.newm.ardrive.turbo.model

fun TokenType.toApiValue(): String =
    when (this) {
        TokenType.ARWEAVE -> "arweave"
        TokenType.ARIO -> "ario"
        TokenType.BASE_ARIO -> "base-ario"
        TokenType.SOLANA -> "solana"
        TokenType.ETHEREUM -> "ethereum"
        TokenType.KYVE -> "kyve"
        TokenType.MATIC -> "matic"
        TokenType.POL -> "pol"
        TokenType.BASE_ETH -> "base-eth"
        TokenType.USDC -> "usdc"
        TokenType.BASE_USDC -> "base-usdc"
        TokenType.POLYGON_USDC -> "polygon-usdc"
        TokenType.ED25519 -> "ed25519"
    }
