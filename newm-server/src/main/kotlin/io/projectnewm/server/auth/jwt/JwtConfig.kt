package io.projectnewm.server.auth.jwt

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.auth.AuthScheme
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.parseAuthorizationHeader
import io.ktor.server.sessions.sessions
import io.projectnewm.server.ext.getConfigString
import io.projectnewm.server.ext.toUUID
import io.projectnewm.server.koin.inject
import io.projectnewm.server.sessions.token
import io.projectnewm.server.user.UserRepository

fun Authentication.Configuration.configureJwt() {
    val repository: UserRepository by inject()
    val environment: ApplicationEnvironment by inject()

    val realm = environment.getConfigString("jwt.realm")
    val issuer = environment.getConfigString("jwt.issuer")
    val audience = environment.getConfigString("jwt.audience")
    val secret = environment.getConfigString("jwt.secret")

    jwt("auth-jwt") {
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
        authHeader { call ->
            call.request.parseAuthorizationHeader() ?: call.sessions.token?.let {
                HttpAuthHeader.Single(AuthScheme.Bearer, it)
            }
        }
    }
}
