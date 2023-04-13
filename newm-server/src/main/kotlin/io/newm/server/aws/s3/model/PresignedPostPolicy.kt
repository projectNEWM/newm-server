package io.newm.server.aws.s3.model

import kotlinx.serialization.Serializable

@Serializable
data class PreSignedPostPolicy(val expiration: String, val conditions: List<Condition>)