package io.newm.server.features.song.model

import kotlinx.serialization.Serializable

@Serializable
data class UploadAudioRequest(
    val fileName: String
)
