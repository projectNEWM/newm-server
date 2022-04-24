package io.projectnewm.server.features.playlist.model

import io.projectnewm.server.serialization.LocalDateTimeSerializer
import io.projectnewm.server.serialization.UUIDSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class Playlist(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime? = null,
    @Serializable(with = UUIDSerializer::class)
    val ownerId: UUID? = null,
    val name: String? = null
)
