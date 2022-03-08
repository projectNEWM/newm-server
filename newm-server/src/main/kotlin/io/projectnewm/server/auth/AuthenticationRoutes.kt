package io.projectnewm.server.auth

import io.ktor.server.application.call
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.sessions.sessions
import io.projectnewm.server.auth.oauth.createOAuthRoutes
import io.projectnewm.server.oauth.OAuthType
import io.projectnewm.server.sessions.clear

fun Routing.createAuthenticationRoutes() {
    createOAuthRoutes(OAuthType.Google)
    createOAuthRoutes(OAuthType.Facebook)
    createOAuthRoutes(OAuthType.LinkedIn)

    get("/logout") {
        call.sessions.clear()
        call.respondRedirect("/")
    }
}
