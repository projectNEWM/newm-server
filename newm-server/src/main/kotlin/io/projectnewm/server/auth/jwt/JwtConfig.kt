package io.projectnewm.server.auth.jwt

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.projectnewm.server.di.inject
import io.projectnewm.server.ext.getConfigString
import io.projectnewm.server.ext.toUUID
import io.projectnewm.server.features.user.repo.UserRepository

const val AUTH_JWT = "auth-jwt"

fun Authentication.Configuration.configureJwt() {
    val repository: UserRepository by inject()
    val environment: ApplicationEnvironment by inject()

    val realm = environment.getConfigString("jwt.realm")
    val issuer = environment.getConfigString("jwt.issuer")
    val audience = environment.getConfigString("jwt.audience")
    val secret = environment.getConfigString("jwt.secret")

    jwt(AUTH_JWT) {
        this.realm = realm
        verifier(
            verifier = JWT.require(Algorithm.HMAC256(secret))
                .withAudience(audience)
                .withIssuer(issuer)
                .build()
        )
        validate { credential ->
            credential.payload.subject?.takeIf { repository.exists(it.toUUID()) }?.let {
                JWTPrincipal(credential.payload)
            }
        }
    }
}
