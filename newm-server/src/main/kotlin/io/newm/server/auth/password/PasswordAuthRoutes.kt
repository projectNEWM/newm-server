package io.newm.server.auth.password

import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import io.newm.server.auth.AUTH_PATH
import io.newm.server.auth.jwt.repo.JwtRepository
import io.newm.server.di.inject
import io.newm.server.features.user.repo.UserRepository

fun Routing.createPasswordAuthRoutes() {
    val userRepository: UserRepository by inject()
    val jwtRepository: JwtRepository by inject()

    post("$AUTH_PATH/login") {
        with(call) {
            val (email, password) = receive<LoginRequest>()
            respond(jwtRepository.createLoginResponse(userRepository.find(email, password)))
        }
    }
}
