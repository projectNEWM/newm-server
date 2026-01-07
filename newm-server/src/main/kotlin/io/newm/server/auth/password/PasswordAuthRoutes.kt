package io.newm.server.auth.password

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.plugins.origin
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.newm.server.auth.AUTH_PATH
import io.newm.server.auth.jwt.repo.JwtRepository
import io.newm.server.config.repo.ConfigRepository
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_RECAPTCHA_IP_WHITELIST
import io.newm.server.features.user.repo.UserRepository
import io.newm.server.recaptcha.repo.RecaptchaRepository
import io.newm.shared.koin.inject
import io.newm.shared.ktx.post

private val logger by lazy { KotlinLogging.logger {} }

fun Routing.createPasswordAuthRoutes() {
    val recaptchaRepository: RecaptchaRepository by inject()
    val userRepository: UserRepository by inject()
    val jwtRepository: JwtRepository by inject()
    val configRepository: ConfigRepository by inject()

    post("$AUTH_PATH/login") {
        val clientIp = request.origin.remoteHost
        val whitelist = configRepository.getCidrWhitelist(CONFIG_KEY_RECAPTCHA_IP_WHITELIST)

        if (!configRepository.isIpInCidrWhitelist(clientIp, whitelist)) {
            recaptchaRepository.verify("login", request)
        }

        val (email, password) = receive<LoginRequest>()
        val (uuid, admin) = userRepository.find(email, password)
        respond(jwtRepository.createLoginResponse(uuid, admin))
    }
}
