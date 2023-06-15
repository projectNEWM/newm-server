package io.newm.server.features.song.model

import kotlinx.serialization.Serializable

@Serializable
data class AudioStreamResponse(
    val url: String,
    val cookies: Map<String, String>
) {
    constructor(data: AudioStreamData) : this(data.url, data.cookies)
}

interface AudioStreamData {
    val url: String
    val cookies: Map<String, String>
}
