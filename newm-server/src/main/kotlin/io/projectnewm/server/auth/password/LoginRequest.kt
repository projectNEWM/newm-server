package io.projectnewm.server.auth.password

import io.projectnewm.server.features.user.model.Password
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: Password
)
