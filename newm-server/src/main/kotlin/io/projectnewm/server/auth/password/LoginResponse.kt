package io.projectnewm.server.auth.password

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(val token: String)
