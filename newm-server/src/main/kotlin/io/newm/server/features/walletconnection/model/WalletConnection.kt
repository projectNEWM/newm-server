package io.newm.server.features.walletconnection.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class WalletConnection(
    @Contextual
    val id: UUID,
    @Contextual
    val createdAt: LocalDateTime,
    val address: String,
    val chain: WalletChain,
    val name: String,
    @Deprecated("use address field instead, will be removed in future versions")
    val stakeAddress: String
)
