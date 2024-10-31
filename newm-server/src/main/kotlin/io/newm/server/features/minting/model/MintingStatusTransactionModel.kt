package io.newm.server.features.minting.model

import io.newm.server.typealiases.SongId
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.*

@Serializable
data class MintingStatusTransactionModel(
    @Contextual
    val id: UUID? = null,
    @Contextual
    val createdAt: LocalDateTime,
    val mintingStatus: String,
    val logMessage: String? = null,
    @Contextual
    val songId: SongId,
)
