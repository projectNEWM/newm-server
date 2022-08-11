package io.newm.server.auth.password

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: Password
)
