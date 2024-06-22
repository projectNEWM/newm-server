package io.newm.server.features.song.model

import io.newm.server.typealiases.SongId
import io.newm.shared.serialization.LocalDateTimeSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class SongReceipt(
    @Contextual
    val id: UUID? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    @Contextual
    val songId: SongId,
    val adaPrice: Long,
    val usdPrice: Long,
    val adaDspPrice: Long,
    val usdDspPrice: Long,
    val adaMintPrice: Long,
    val usdMintPrice: Long,
    val adaCollabPrice: Long,
    val usdCollabPrice: Long,
    val usdAdaExchangeRate: Long,
)
