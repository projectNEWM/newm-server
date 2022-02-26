package io.projectnewm.server.pugins.auth

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
import io.projectnewm.server.ext.getConfigLong
import io.projectnewm.server.ext.getConfigString
import io.projectnewm.server.pugins.token
import java.util.Date

fun Authentication.Configuration.configureJwt(environment: ApplicationEnvironment) {

    val issuer = environment.getConfigString("jwt.issuer")
    val audience = environment.getConfigString("jwt.audience")
    val secret = environment.getConfigString("jwt.secret")

    jwt("auth-jwt") {
        verifier(
            verifier = JWT.require(Algorithm.HMAC256(secret))
                .withAudience(audience)
                .withIssuer(issuer)
                .build()
        )
        validate { credential ->
            credential.payload.subject?.let { JWTPrincipal(credential.payload) }
        }
        authHeader { call ->
            call.request.parseAuthorizationHeader() ?: call.sessions.token?.let {
                HttpAuthHeader.Single(AuthScheme.Bearer, it)
            }
        }
    }
}

fun createJwtToken(subject: String, environment: ApplicationEnvironment): String {
    return with(environment) {
        JWT.create()
            .withIssuer(getConfigString("jwt.issuer"))
            .withAudience(getConfigString("jwt.audience"))
            .withSubject(subject)
            .withExpiresAt(Date(System.currentTimeMillis() + getConfigLong("jwt.timeToLive")))
            .sign(Algorithm.HMAC256(getConfigString("jwt.secret")))
    }
}
