package io.projectnewm.server.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.sessions.sessions
import io.projectnewm.server.auth.jwt.createJwtRoutes
import io.projectnewm.server.auth.oauth.OAuthType
import io.projectnewm.server.auth.oauth.createOAuthRoutes
import io.projectnewm.server.auth.password.createPasswordAuthRoutes
import io.projectnewm.server.auth.twofactor.createTwoFactorAuthRoutes
import io.projectnewm.server.sessions.token

fun Routing.createAuthenticationRoutes() {
    createPasswordAuthRoutes()
    createTwoFactorAuthRoutes()
    createOAuthRoutes(OAuthType.Google)
    createOAuthRoutes(OAuthType.Facebook)
    createOAuthRoutes(OAuthType.LinkedIn)
    createJwtRoutes()

    authenticate("auth-jwt") {
        get("/logout") {
            call.sessions.token = null
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
