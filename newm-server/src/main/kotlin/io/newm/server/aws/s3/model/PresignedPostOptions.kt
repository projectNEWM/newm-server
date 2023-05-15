package io.newm.server.aws.s3.model

import com.amazonaws.auth.AWSCredentials

class PresignedPostOptions(
    val bucket: String,
    val key: String,
    val credentials: AWSCredentials,
    val conditions: List<Condition> = emptyList(),
    val fields: Map<String, String> = emptyMap(),
    val expiresSeconds: Long = 3600L
)

class PresignedPostOptionBuilder {
    var bucket: String? = null
    var key: String? = null
    var credentials: AWSCredentials? = null
    var conditions: List<Condition> = emptyList()
    val fields: Map<String, String> = emptyMap()
    var expiresSeconds: Long = 3600L

    fun options(): PresignedPostOptions {
        return PresignedPostOptions(
            bucket = bucket ?: throw IllegalArgumentException("bucket is required"),
            key = key ?: throw IllegalArgumentException("key is required"),
            credentials = credentials ?: throw IllegalArgumentException("credentials is required"),
            conditions = conditions,
            fields = fields,
            expiresSeconds = expiresSeconds
        )
    }
}
