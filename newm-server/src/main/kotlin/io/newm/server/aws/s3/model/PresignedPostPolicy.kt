package io.newm.server.aws.s3.model

import kotlinx.serialization.Serializable

@Serializable
data class PresignedPostPolicy(val expiration: String, val conditions: List<Condition>)
