package io.projectnewm.server.auth.jwt.repo

import io.projectnewm.server.auth.jwt.JwtType
import java.util.UUID

interface JwtRepository {
    suspend fun create(type: JwtType, userId: UUID): String
    suspend fun delete(jwtId: UUID)
    suspend fun exists(jwtId: UUID): Boolean
}
