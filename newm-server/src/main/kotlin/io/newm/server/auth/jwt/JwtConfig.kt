package io.newm.server.auth.jwt

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.newm.server.auth.jwt.repo.JwtRepository
import io.newm.server.ktx.getSecureConfigString
import io.newm.shared.koin.inject
import io.newm.shared.ktx.getConfigString
import io.newm.shared.ktx.toUUID
import java.time.Instant
import kotlinx.coroutines.runBlocking

const val AUTH_JWT = "auth-jwt"
const val AUTH_JWT_REFRESH = "auth-jwt-refresh"
const val AUTH_JWT_ADMIN = "auth-jwt-admin"

fun AuthenticationConfig.configureJwt() {
    configureJwt(AUTH_JWT, JwtType.Access)
    configureJwt(AUTH_JWT_REFRESH, JwtType.Refresh)
    configureJwt(AUTH_JWT_ADMIN, JwtType.Access)
}

private fun AuthenticationConfig.configureJwt(
    name: String,
    type: JwtType
) {
    val environment: ApplicationEnvironment by inject()
    val repository: JwtRepository by inject()
    val logger = KotlinLogging.logger {}

    jwt(name) {
        this.realm = environment.getConfigString("jwt.realm")
        verifier(
            JWT
                .require(Algorithm.HMAC256(runBlocking { environment.getSecureConfigString("jwt.secret") }))
                .withAudience(environment.getConfigString("jwt.audience"))
                .withIssuer(environment.getConfigString("jwt.issuer"))
                .withClaim("type", type.name)
                .apply {
                    if (name == AUTH_JWT_ADMIN) {
                        withClaim("admin", true)
                    }
                }.build()
        )
        validate { credential ->
            with(credential) {
                val blacklisted = payload.id?.let { jwtId -> repository.isBlacklisted(jwtId.toUUID()) } ?: true
                val expired = payload.expiresAtAsInstant?.isBefore(Instant.now()) ?: true
                val admin = payload.getClaim("admin")?.asBoolean() == true

                val isValid =
                    if (name == AUTH_JWT_ADMIN) {
                        !blacklisted && !expired && admin
                    } else {
                        !blacklisted && !expired
                    }

                if (isValid) {
                    JWTPrincipal(payload)
                } else {
                    logger.warn {
                        "JWT Auth Failed: audience: $audience, issuer: $issuer, issuedAt: $issuedAt, expiresAt: $expiresAt, admin: $admin"
                    }
                    null
                }
            }
        }
    }
}
