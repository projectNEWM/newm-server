package io.newm.server.features.minting

import io.newm.server.features.song.model.MintingStatus
import io.newm.server.typealiases.SongId
import io.newm.shared.serialization.UUIDSerializer
import kotlinx.serialization.Serializable

@Serializable
data class MintingStatusSqsMessage(
    @Serializable(with = UUIDSerializer::class)
    val songId: SongId,
    val mintingStatus: MintingStatus,
)
