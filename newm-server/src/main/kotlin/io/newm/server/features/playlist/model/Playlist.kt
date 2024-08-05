package io.newm.server.features.playlist.model

import io.newm.server.typealiases.UserId
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class Playlist(
    @Contextual
    val id: UUID? = null,
    @Contextual
    val createdAt: LocalDateTime? = null,
    @Contextual
    val ownerId: UserId? = null,
    val name: String? = null
)
