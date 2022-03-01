package io.projectnewm.server.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.sessions.sessions
import io.projectnewm.server.oauth.OAuthType
import io.projectnewm.server.plugins.auth.configureJwt
import io.projectnewm.server.plugins.auth.configureOAuth
import io.projectnewm.server.plugins.auth.routeOAuth
import io.projectnewm.server.sessions.clear

fun Application.configureAuthentication() {
    install(Authentication) {
        configureOAuth(OAuthType.Google, environment)
        configureOAuth(OAuthType.Facebook, environment)
        configureOAuth(OAuthType.LinkedIn, environment)
        configureJwt(environment)
    }
    routing {
        routeOAuth(OAuthType.Google)
        routeOAuth(OAuthType.Facebook)
        routeOAuth(OAuthType.LinkedIn)

        get("/logout") {
            call.sessions.clear()
            call.respondRedirect("/")
        }
    }
}
