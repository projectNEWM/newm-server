package io.newm.server.features.song.model

import io.newm.server.aws.s3.model.PresignedPost
import kotlinx.serialization.Serializable

@Serializable
data class AudioUploadResponse(
    val url: String,
    val fields: Map<String, String>
) {
    constructor(presignedPost: PresignedPost) : this(presignedPost.url, presignedPost.fields)
}
