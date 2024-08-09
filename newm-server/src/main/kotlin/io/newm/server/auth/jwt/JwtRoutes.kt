package io.newm.server.auth.jwt

import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.newm.server.auth.AUTH_PATH
import io.newm.server.auth.jwt.repo.JwtRepository
import io.newm.server.auth.password.createLoginResponse
import io.newm.server.features.user.repo.UserRepository
import io.newm.server.ktx.jwtId
import io.newm.server.ktx.jwtPrincipal
import io.newm.server.ktx.myUserId
import io.newm.shared.koin.inject
import io.newm.shared.ktx.get

fun Routing.createJwtRoutes() {
    val jwtRepository: JwtRepository by inject()
    val userRepository: UserRepository by inject()

    authenticate(AUTH_JWT) {
        get("$AUTH_PATH/jwt") {
            with(jwtPrincipal) {
                respond(
                    JwtData(
                        id = jwtId.toString(),
                        issuer = issuer.toString(),
                        audience = audience.toString(),
                        subject = subject.toString(),
                        expiresAt = expiresAt.toString()
                    )
                )
            }
        }
    }
    authenticate(AUTH_JWT_REFRESH) {
        get("$AUTH_PATH/refresh") {
            jwtId?.let { jwtRepository.blackList(it) }
            val admin = userRepository.isAdmin(myUserId)
            respond(jwtRepository.createLoginResponse(myUserId, admin))
        }
    }
}
