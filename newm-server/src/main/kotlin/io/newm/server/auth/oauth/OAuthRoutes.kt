package io.newm.server.auth.oauth

import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.newm.server.auth.AUTH_PATH
import io.newm.server.auth.jwt.repo.JwtRepository
import io.newm.server.auth.oauth.model.OAuthLoginRequest
import io.newm.server.auth.oauth.model.OAuthType
import io.newm.server.auth.oauth.repo.OAuthRepository
import io.newm.server.auth.password.createLoginResponse
import io.newm.shared.koin.inject
import io.newm.shared.exception.HttpBadRequestException
import io.newm.server.features.user.repo.UserRepository
import io.newm.server.recaptcha.repo.RecaptchaRepository
import io.newm.shared.ktx.post

fun Routing.createOAuthRoutes(type: OAuthType) {
    val recaptchaRepository: RecaptchaRepository by inject()
    val userRepository: UserRepository by inject()
    val oAuthRepository: OAuthRepository by inject()
    val jwtRepository: JwtRepository by inject()

    val typeName = type.name.lowercase()
    post("$AUTH_PATH/login/$typeName") {
        recaptchaRepository.verify("login_$typeName", request)
        val req = receive<OAuthLoginRequest>()
        val oauthTokens =
            req.oauthTokens ?: req.code?.let { code ->
                oAuthRepository.getTokens(type, code, req.redirectUri)
            } ?: throw HttpBadRequestException("missing code")
        respond(jwtRepository.createLoginResponse(userRepository.findOrAdd(type, oauthTokens)))
    }
}
