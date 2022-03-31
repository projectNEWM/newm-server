package io.projectnewm.server.auth

import io.ktor.server.routing.Routing
import io.projectnewm.server.auth.jwt.createJwtRoutes
import io.projectnewm.server.auth.oauth.OAuthType
import io.projectnewm.server.auth.oauth.createOAuthRoutes
import io.projectnewm.server.auth.password.createPasswordAuthRoutes
import io.projectnewm.server.auth.twofactor.createTwoFactorAuthRoutes

fun Routing.createAuthenticationRoutes() {
    createPasswordAuthRoutes()
    createTwoFactorAuthRoutes()
    createOAuthRoutes(OAuthType.Google)
    createOAuthRoutes(OAuthType.Facebook)
    createOAuthRoutes(OAuthType.LinkedIn)
    createJwtRoutes()
}
