package io.projectnewm.server.features.song.model

import kotlinx.serialization.Serializable

@Serializable
data class UploadRequest(
    val fileName: String
)
