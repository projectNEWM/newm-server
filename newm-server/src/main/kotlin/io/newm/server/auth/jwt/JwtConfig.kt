package io.newm.server.auth.jwt

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.newm.server.auth.jwt.repo.JwtRepository
import io.newm.shared.koin.inject
import io.newm.shared.ktx.getConfigString
import io.newm.shared.ktx.toUUID

const val AUTH_JWT = "auth-jwt"
const val AUTH_JWT_REFRESH = "auth-jwt-refresh"

fun AuthenticationConfig.configureJwt() {
    configureJwt(AUTH_JWT, JwtType.Access)
    configureJwt(AUTH_JWT_REFRESH, JwtType.Refresh)
}

private fun AuthenticationConfig.configureJwt(name: String, type: JwtType) {
    val environment: ApplicationEnvironment by inject()
    val repository: JwtRepository by inject()

    jwt(name) {
        this.realm = environment.getConfigString("jwt.realm")
        verifier(
            JWT.require(Algorithm.HMAC256(environment.getConfigString("jwt.secret")))
                .withAudience(environment.getConfigString("jwt.audience"))
                .withIssuer(environment.getConfigString("jwt.issuer"))
                .withClaim("type", type.name)
                .build()
        )
        validate { credential ->
            with(credential) {
                payload.id?.takeIf { jwkId ->
                    repository.exists(jwkId.toUUID())
                }?.let {
                    JWTPrincipal(payload)
                }
            }
        }
    }
}
