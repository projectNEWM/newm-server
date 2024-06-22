package io.newm.server.features.song.model

import io.newm.server.typealiases.SongId
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class SongIdBody(
    @Contextual
    val songId: SongId
)
