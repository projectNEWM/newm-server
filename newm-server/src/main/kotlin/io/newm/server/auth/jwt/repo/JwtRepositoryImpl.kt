package io.newm.server.auth.jwt.repo

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.github.benmanes.caffeine.cache.Caffeine
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.ApplicationEnvironment
import io.newm.server.auth.jwt.JwtType
import io.newm.server.ktx.getSecureConfigString
import io.newm.server.typealiases.UserId
import io.newm.shared.ktx.getConfigLong
import io.newm.shared.ktx.getConfigString
import java.security.SecureRandom
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration.Companion.days
import kotlin.time.toJavaDuration

class JwtRepositoryImpl(
    private val environment: ApplicationEnvironment
) : JwtRepository {
    private val logger = KotlinLogging.logger {}
    private val secureRandom by lazy { SecureRandom.getInstanceStrong() }
    private val blackList = Caffeine.newBuilder().expireAfterWrite(7.days.toJavaDuration()).build<UUID, Boolean>()

    override suspend fun create(
        type: JwtType,
        userId: UserId,
        admin: Boolean
    ): String {
        logger.debug { "create: type = $type, userId = $userId" }

        val expiresAt = Instant.now().plusSeconds(environment.getConfigLong("jwt.${type.name.lowercase()}.timeToLive"))

        val jwtId = UUID.randomUUID().toString()

        return JWT
            .create()
            .withJWTId(jwtId)
            .withIssuer(environment.getConfigString("jwt.issuer"))
            .withAudience(environment.getConfigString("jwt.audience"))
            .withSubject(userId.toString())
            .withExpiresAt(expiresAt)
            .withClaim("type", type.name)
            .withClaim("admin", admin)
            .sign(Algorithm.HMAC256(environment.getSecureConfigString("jwt.secret")))
    }

    override fun blackList(jwtId: UUID) = blackList.put(jwtId, true)

    override fun isBlacklisted(jwtId: UUID): Boolean = blackList.getIfPresent(jwtId) != null
}
