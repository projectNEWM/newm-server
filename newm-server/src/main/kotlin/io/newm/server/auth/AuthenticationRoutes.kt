package io.newm.server.auth

import io.ktor.server.routing.Routing
import io.newm.server.auth.jwt.createJwtRoutes
import io.newm.server.auth.oauth.OAuthType
import io.newm.server.auth.oauth.createOAuthRoutes
import io.newm.server.auth.password.createPasswordAuthRoutes
import io.newm.server.auth.twofactor.createTwoFactorAuthRoutes

const val AUTH_PATH = "v1/auth"

fun Routing.createAuthenticationRoutes() {
    createPasswordAuthRoutes()
    createTwoFactorAuthRoutes()
    createOAuthRoutes(OAuthType.Google)
    createOAuthRoutes(OAuthType.Facebook)
    createOAuthRoutes(OAuthType.LinkedIn)
    createJwtRoutes()
}
