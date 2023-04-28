package io.newm.server.auth.password

import io.newm.server.auth.jwt.JwtType
import io.newm.server.auth.jwt.repo.JwtRepository
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class LoginResponse(
    val accessToken: String,
    val refreshToken: String
)

suspend fun JwtRepository.createLoginResponse(userId: UUID, admin: Boolean = false) = LoginResponse(
    accessToken = create(JwtType.Access, userId, admin),
    refreshToken = create(JwtType.Refresh, userId, admin)
)
