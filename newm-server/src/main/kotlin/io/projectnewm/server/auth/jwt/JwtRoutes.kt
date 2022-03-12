package io.projectnewm.server.auth.jwt

import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get

fun Routing.createJwtRoutes() {
    authenticate("auth-jwt") {
        get("/auth/jwt") {
            val principal = call.principal<JWTPrincipal>()!!
            call.respond(
                JwtData(
                    issuer = principal.issuer.toString(),
                    audience = principal.audience.toString(),
                    subject = principal.subject.toString(),
                    expiresAt = principal.expiresAt.toString()
                )
            )
        }
    }
}
