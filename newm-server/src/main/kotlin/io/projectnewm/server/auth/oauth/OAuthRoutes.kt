package io.projectnewm.server.auth.oauth

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.OAuthAccessTokenResponse
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.sessions.sessions
import io.projectnewm.server.auth.jwt.createJwt
import io.projectnewm.server.ext.requiredQueryParam
import io.projectnewm.server.koin.inject
import io.projectnewm.server.sessions.redirectUrl
import io.projectnewm.server.sessions.token
import io.projectnewm.server.user.UserRepository

fun Routing.createOAuthRoutes(type: OAuthType) {
    val repository: UserRepository by inject()
    val name = type.name.lowercase()

    post("/login/$name") {
        with(call) {
            val req: OAuthLoginRequest = receive()
            val token = application.createJwt(
                userId = repository.findOrAdd(type, req.accessToken)
            )
            respond(OAuthLoginResponse(token))
        }
    }

    get("/login/$name") {
        call.sessions.redirectUrl = call.request.requiredQueryParam("redirectUrl")
        call.respondRedirect("/auth/$name")
    }

    authenticate("auth-oauth-$name") {
        get("/auth/$name") {
            with(call) {
                principal<OAuthAccessTokenResponse.OAuth2>()?.accessToken?.let { accessToken ->
                    sessions.token = application.createJwt(repository.findOrAdd(type, accessToken))
                    sessions.redirectUrl?.let { respondRedirect(it) } ?: respond(HttpStatusCode.NoContent)
                } ?: respond(HttpStatusCode.Unauthorized)
                sessions.redirectUrl = null
            }
        }
    }
}
