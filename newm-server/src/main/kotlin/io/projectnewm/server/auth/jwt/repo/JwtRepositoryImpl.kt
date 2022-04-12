package io.projectnewm.server.auth.jwt.repo

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.util.logging.Logger
import io.projectnewm.server.auth.jwt.JwtType
import io.projectnewm.server.auth.jwt.database.JwtEntity
import io.projectnewm.server.ext.exists
import io.projectnewm.server.ext.getConfigLong
import io.projectnewm.server.ext.getConfigString
import io.projectnewm.server.ext.toDate
import io.projectnewm.server.features.user.database.UserTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.MarkerFactory
import java.time.LocalDateTime
import java.util.UUID

class JwtRepositoryImpl(
    private val environment: ApplicationEnvironment,
    private val logger: Logger
) : JwtRepository {

    private val marker = MarkerFactory.getMarker(javaClass.simpleName)

    override suspend fun create(type: JwtType, userId: UUID): String {
        logger.debug(marker, "create: type = $type, userId = $userId")

        val expiresAt = LocalDateTime
            .now()
            .plusMinutes(environment.getConfigLong("jwt.${type.name.lowercase()}.timeToLive"))

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

    override suspend fun delete(jwtId: UUID): Unit = transaction {
        logger.debug(marker, "delete: jwtId = $jwtId")
        JwtEntity.deleteAllExpired()
        JwtEntity[jwtId].delete()
    }

    override suspend fun exists(jwtId: UUID): Boolean = transaction {
        logger.debug(marker, "exists: jwtId = $jwtId")
        JwtEntity.deleteAllExpired()
        JwtEntity.exists(jwtId)
    }
}
