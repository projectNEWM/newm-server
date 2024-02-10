package io.newm.server.auth.password

import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.newm.server.auth.AUTH_PATH
import io.newm.server.auth.jwt.repo.JwtRepository
import io.newm.server.features.user.repo.UserRepository
import io.newm.server.recaptcha.repo.RecaptchaRepository
import io.newm.shared.koin.inject
import io.newm.shared.ktx.post

fun Routing.createPasswordAuthRoutes() {
    val recaptchaRepository: RecaptchaRepository by inject()
    val userRepository: UserRepository by inject()
    val jwtRepository: JwtRepository by inject()

    post("$AUTH_PATH/login") {
        recaptchaRepository.verify("login", request)
        val (email, password) = receive<LoginRequest>()
        val (uuid, admin) = userRepository.find(email, password)
        respond(jwtRepository.createLoginResponse(uuid, admin))
    }
}
