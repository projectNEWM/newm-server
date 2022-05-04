package io.projectnewm.server.features.playlist.model

import io.projectnewm.server.serialization.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class PlaylistIdBody(
    @Serializable(with = UUIDSerializer::class)
    val playlistId: UUID
)
