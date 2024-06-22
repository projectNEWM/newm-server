package io.newm.server.features.playlist.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class PlaylistIdBody(
    @Contextual
    val playlistId: UUID
)
