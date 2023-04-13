package io.newm.server.aws.s3.model

import kotlinx.serialization.Serializable

@Serializable
data class PresignedPost(
    val url: String,
    val fields: Map<String, String>
)