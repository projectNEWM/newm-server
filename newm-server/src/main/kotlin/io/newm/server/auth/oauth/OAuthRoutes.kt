package io.newm.server.auth.oauth

import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import io.newm.server.auth.AUTH_PATH
import io.newm.server.auth.jwt.repo.JwtRepository
import io.newm.server.auth.oauth.repo.OAuthRepository
import io.newm.server.auth.password.createLoginResponse
import io.newm.shared.koin.inject
import io.newm.shared.exception.HttpBadRequestException
import io.newm.server.features.user.repo.UserRepository

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
