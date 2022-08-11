package io.projectnewm.server.auth.jwt

import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.projectnewm.server.auth.AUTH_PATH
import io.projectnewm.server.auth.jwt.repo.JwtRepository
import io.projectnewm.server.auth.password.createLoginResponse
import io.projectnewm.server.di.inject
import io.projectnewm.server.ext.jwtId
import io.projectnewm.server.ext.jwtPrincipal
import io.projectnewm.server.ext.myUserId

fun Routing.createJwtRoutes() {
    val jwtRepository: JwtRepository by inject()

    authenticate(AUTH_JWT) {
        get("$AUTH_PATH/jwt") {
            val principal = call.jwtPrincipal
            call.respond(
                JwtData(
                    id = principal.jwtId.toString(),
                    issuer = principal.issuer.toString(),
                    audience = principal.audience.toString(),
                    subject = principal.subject.toString(),
                    expiresAt = principal.expiresAt.toString()
                )
            )
        }
    }
    authenticate(AUTH_JWT_REFRESH) {
        get("$AUTH_PATH/refresh") {
            with(call) {
                jwtRepository.delete(jwtId)
                respond(jwtRepository.createLoginResponse(myUserId))
            }
        }
    }
}
