package io.projectnewm.server.auth.oauth

import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import io.projectnewm.server.auth.AUTH_PATH
import io.projectnewm.server.auth.jwt.repo.JwtRepository
import io.projectnewm.server.auth.oauth.repo.OAuthRepository
import io.projectnewm.server.auth.password.createLoginResponse
import io.projectnewm.server.di.inject
import io.projectnewm.server.exception.HttpBadRequestException
import io.projectnewm.server.features.user.repo.UserRepository

fun Routing.createOAuthRoutes(type: OAuthType) {
    val userRepository: UserRepository by inject()
    val oAuthRepository: OAuthRepository by inject()
    val jwtRepository: JwtRepository by inject()

    post("$AUTH_PATH/login/${type.name.lowercase()}") {
        with(call) {
            val req = receive<OAuthLoginRequest>()
            val oauthAccessToken = req.accessToken ?: req.code?.let { code ->
                req.redirectUri?.let { redirectUri ->
                    oAuthRepository.getAccessToken(type, code, redirectUri)
                } ?: throw HttpBadRequestException("missing redirectUri")
            } ?: throw HttpBadRequestException("missing code")
            respond(jwtRepository.createLoginResponse(userRepository.findOrAdd(type, oauthAccessToken)))
        }
    }
}
