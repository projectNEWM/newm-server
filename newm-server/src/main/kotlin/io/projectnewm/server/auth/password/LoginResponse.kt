package io.projectnewm.server.auth.password

import io.projectnewm.server.auth.jwt.JwtType
import io.projectnewm.server.auth.jwt.repo.JwtRepository
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
