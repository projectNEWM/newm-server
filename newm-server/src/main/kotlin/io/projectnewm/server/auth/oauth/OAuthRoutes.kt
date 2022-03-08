package io.projectnewm.server.auth.oauth

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.OAuthAccessTokenResponse
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.sessions.sessions
import io.projectnewm.server.auth.jwt.createJwt
import io.projectnewm.server.koin.inject
import io.projectnewm.server.oauth.OAuthType
import io.projectnewm.server.sessions.token
import io.projectnewm.server.user.UserRepository

fun Routing.createOAuthRoutes(type: OAuthType) {
    val repository: UserRepository by inject()
    val name = type.name.lowercase()

    authenticate("auth-oauth-$name") {
        get("/login/$name") {
            // Redirects to 'authorizeUrl' automatically
        }
        get("/auth/$name") {
            with(call) {
                val accessToken = principal<OAuthAccessTokenResponse.OAuth2>()?.accessToken
                if (accessToken != null) {
                    call.sessions.token = application.createJwt(
                        repository.findOrAdd(type, accessToken).toString()
                    )
                    respondRedirect("/")
                } else {
                    response.status(HttpStatusCode.Unauthorized)
                }
            }
        }
    }
}
