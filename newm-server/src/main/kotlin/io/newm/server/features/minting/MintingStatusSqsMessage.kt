package io.newm.server.features.minting

import io.newm.server.features.song.model.MintingStatus
import io.newm.shared.serialization.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class MintingStatusSqsMessage(
    @Serializable(with = UUIDSerializer::class)
    val songId: UUID,
    val mintingStatus: MintingStatus,
)
