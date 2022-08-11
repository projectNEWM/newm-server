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

suspend fun JwtRepository.createLoginResponse(userId: UUID) = LoginResponse(
    accessToken = create(JwtType.Access, userId),
    refreshToken = create(JwtType.Refresh, userId)
)
