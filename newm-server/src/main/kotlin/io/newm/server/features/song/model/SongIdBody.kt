package io.projectnewm.server.features.song.model

import io.projectnewm.server.serialization.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class SongIdBody(
    @Serializable(with = UUIDSerializer::class)
    val songId: UUID
)
