package io.projectnewm.server.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.sessions.sessions
import io.projectnewm.server.auth.jwt.createJwtRoutes
import io.projectnewm.server.auth.oauth.OAuthType
import io.projectnewm.server.auth.oauth.createOAuthRoutes
import io.projectnewm.server.auth.password.createPasswordAuthRoutes
import io.projectnewm.server.auth.twofactor.createTwoFactorAuthRoutes
import io.projectnewm.server.sessions.clear

fun Routing.createAuthenticationRoutes() {
    createPasswordAuthRoutes()
    createTwoFactorAuthRoutes()
    createOAuthRoutes(OAuthType.Google)
    createOAuthRoutes(OAuthType.Facebook)
    createOAuthRoutes(OAuthType.LinkedIn)
    createJwtRoutes()

    get("/logout") {
        call.sessions.clear()
        call.respond(HttpStatusCode.NoContent)
    }
}
