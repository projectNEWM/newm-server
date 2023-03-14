package io.newm.server.features.playlist.model

import io.newm.shared.serialization.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class PlaylistIdBody(
    @Serializable(with = UUIDSerializer::class)
    val playlistId: UUID
)
