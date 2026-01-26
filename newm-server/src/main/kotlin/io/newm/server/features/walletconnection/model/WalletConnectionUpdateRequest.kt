package io.newm.server.features.walletconnection.model

import kotlinx.serialization.Serializable

@Serializable
data class WalletConnectionUpdateRequest(
    val name: String
)
