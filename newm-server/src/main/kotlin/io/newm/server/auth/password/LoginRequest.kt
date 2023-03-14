package io.newm.server.auth.password

import io.newm.shared.auth.Password
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: Password
)
