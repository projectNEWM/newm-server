package io.newm.server.auth.jwt.repo

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.util.logging.Logger
import io.newm.server.auth.jwt.JwtType
import io.newm.server.auth.jwt.database.JwtEntity
import io.newm.server.features.user.database.UserTable
import io.newm.shared.koin.inject
import io.newm.shared.ktx.debug
import io.newm.shared.ktx.existsHavingId
import io.newm.shared.ktx.getConfigLong
import io.newm.shared.ktx.getConfigString
import io.newm.shared.ktx.toDate
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.parameter.parametersOf
import java.time.LocalDateTime
import java.util.UUID

class JwtRepositoryImpl(
    private val environment: ApplicationEnvironment
) : JwtRepository {

    private val logger: Logger by inject { parametersOf(javaClass.simpleName) }

    override suspend fun create(type: JwtType, userId: UUID): String {
        logger.debug { "create: type = $type, userId = $userId" }

        val expiresAt = LocalDateTime
            .now()
            .plusSeconds(environment.getConfigLong("jwt.${type.name.lowercase()}.timeToLive"))

        val jwtId = transaction {
            JwtEntity.deleteAllExpired()
            JwtEntity.new {
                this.userId = EntityID(userId, UserTable)
                this.expiresAt = expiresAt
            }.id.value
        }

        return JWT.create()
            .withJWTId(jwtId.toString())
            .withIssuer(environment.getConfigString("jwt.issuer"))
            .withAudience(environment.getConfigString("jwt.audience"))
            .withSubject(userId.toString())
            .withExpiresAt(expiresAt.toDate())
            .withClaim("type", type.name)
            .sign(Algorithm.HMAC256(environment.getConfigString("jwt.secret")))
    }

    override suspend fun delete(jwtId: UUID) {
        logger.debug { "delete: jwtId = $jwtId" }
        transaction { JwtEntity[jwtId].delete() }
    }

    override suspend fun exists(jwtId: UUID): Boolean {
        logger.debug { "exists: jwtId = $jwtId" }
        return transaction { JwtEntity.existsHavingId(jwtId) }
    }
}
