package io.projectnewm.server.pugins.auth.google

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.http.auth.AuthScheme
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.parseAuthorizationHeader
import io.projectnewm.server.ext.getConfigLong
import io.projectnewm.server.ext.getConfigString
import java.net.URL
import java.util.concurrent.TimeUnit

fun Authentication.Configuration.configureJwtGoogle(environment: ApplicationEnvironment) {

    val jwkProvider = JwkProviderBuilder(URL(environment.getConfigString("jwt.google.certificatesUrl")))
        .cached(
            environment.getConfigLong("jwt.google.cacheSize"),
            environment.getConfigLong("jwt.google.cacheExpiresIn"),
            TimeUnit.HOURS
        )
        .build()

    val audience = environment.getConfigString("jwt.google.audience")

    jwt("auth-jwt-google") {
        verifier(jwkProvider) {
            withIssuer(environment.getConfigString("jwt.google.issuer"))
            withAudience(audience)
        }
        validate { credential ->
            if (credential.payload.audience.contains(audience))
                JWTPrincipal(credential.payload)
            else
                null
        }
        authHeader { call ->
            call.request.parseAuthorizationHeader() ?: call.request.cookies["token"]?.let {
                HttpAuthHeader.Single(AuthScheme.Bearer, it)
            }
        }
    }
}
