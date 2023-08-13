package io.newm.server.features.model

import kotlinx.serialization.Serializable

@Serializable
data class AudioUploadReport(
    val url: String,
    val mimeType: String,
    val fileSize: Long, // in bytes
    val duration: Int, // in seconds
    val sampleRate: Int // in Hz
)
