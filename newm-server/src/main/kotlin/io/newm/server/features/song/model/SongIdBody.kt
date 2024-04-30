package io.newm.server.features.song.model

import io.newm.server.typealiases.SongId
import io.newm.shared.serialization.UUIDSerializer
import kotlinx.serialization.Serializable

@Serializable
data class SongIdBody(
    @Serializable(with = UUIDSerializer::class)
    val songId: SongId
)
