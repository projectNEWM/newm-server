package io.newm.server.features.walletconnection.model

import io.newm.shared.serialization.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
class ConnectResponse(
    @Serializable(with = UUIDSerializer::class)
    val connectionId: UUID,
    val stakeAddress: String
)
