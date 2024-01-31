package io.newm.server.features.song.model

import kotlinx.serialization.Serializable

@Serializable
data class AudioUploadReport(
    val url: String,
    val mimeType: String,
    // in bytes
    val fileSize: Long,
    // in seconds
    val duration: Int,
    // in Hz
    val sampleRate: Int
)
