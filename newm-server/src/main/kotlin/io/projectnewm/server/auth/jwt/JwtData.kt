package io.projectnewm.server.auth.jwt

import kotlinx.serialization.Serializable

@Serializable
data class JwtData(
    val issuer: String,
    val audience: String,
    val subject: String,
    val expiresAt: String
)