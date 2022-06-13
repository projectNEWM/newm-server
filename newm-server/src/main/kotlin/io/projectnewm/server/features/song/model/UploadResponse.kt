package io.projectnewm.server.features.song.model

import kotlinx.serialization.Serializable

@Serializable
data class UploadResponse(
    val uploadUrl: String
)
