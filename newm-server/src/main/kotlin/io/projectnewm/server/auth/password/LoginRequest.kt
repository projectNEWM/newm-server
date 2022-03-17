package io.projectnewm.server.auth.password

import io.projectnewm.server.user.Password
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: Password
)
