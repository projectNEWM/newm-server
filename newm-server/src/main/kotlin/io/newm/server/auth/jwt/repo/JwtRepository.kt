package io.newm.server.auth.jwt.repo

import io.newm.server.auth.jwt.JwtType
import io.newm.server.typealiases.UserId
import java.util.UUID

interface JwtRepository {
    suspend fun create(
        type: JwtType,
        userId: UserId,
        admin: Boolean
    ): String

    suspend fun delete(jwtId: UUID)

    suspend fun exists(jwtId: UUID): Boolean
}
