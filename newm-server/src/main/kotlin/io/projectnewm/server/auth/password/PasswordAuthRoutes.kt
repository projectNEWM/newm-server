package io.projectnewm.server.auth.password

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import io.ktor.server.sessions.sessions
import io.projectnewm.server.auth.jwt.createJwt
import io.projectnewm.server.koin.inject
import io.projectnewm.server.sessions.token
import io.projectnewm.server.user.UserRepository

fun Routing.createPasswordAuthRoutes() {
    val repository: UserRepository by inject()

    post("/login") {
        with(call) {
            val req: LoginRequest = receive()
            val token = application.createJwt(
                userId = repository.find(req.email, req.password)
            )
            if (request.queryParameters.contains("session")) {
                sessions.token = token
                respond(HttpStatusCode.NoContent)
            } else {
                respond(LoginResponse(token))
            }
        }
    }
}
