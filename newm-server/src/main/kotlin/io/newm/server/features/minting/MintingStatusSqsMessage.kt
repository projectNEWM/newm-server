package io.newm.server.features.minting

import io.newm.server.features.song.model.MintingStatus
import io.newm.server.typealiases.SongId
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class MintingStatusSqsMessage(
    @Contextual
    val songId: SongId,
    val mintingStatus: MintingStatus,
)
