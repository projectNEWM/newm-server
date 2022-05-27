package io.projectnewm.server.features.song.model

import io.projectnewm.server.serialization.LocalDateTimeSerializer
import io.projectnewm.server.serialization.UUIDSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class Song(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID? = null,
    @Serializable(with = UUIDSerializer::class)
    val ownerId: UUID? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime? = null,
    val title: String? = null,
    val genre: String? = null,
    val covertArtUrl: String? = null,
    val description: String? = null,
    val credits: String? = null
)
