package io.newm.server.features.song.model

import io.newm.server.typealiases.SongId
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class SongReceipt(
    @Contextual
    val id: UUID? = null,
    @Contextual
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
    val newmPrice: Long,
    val newmDspPrice: Long,
    val newmMintPrice: Long,
    val newmCollabPrice: Long,
    val usdNewmExchangeRate: Long,
)
