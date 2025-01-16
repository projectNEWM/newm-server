package io.newm.server.features.song.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class SongSmartLink(
    @Contextual
    val id: UUID,
    val storeName: String,
    val url: String
)
