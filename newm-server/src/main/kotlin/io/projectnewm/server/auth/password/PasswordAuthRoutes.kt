package io.projectnewm.server.auth.password

import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.projectnewm.server.ext.requiredQueryParam
import io.projectnewm.server.koin.inject
import io.projectnewm.server.user.UserRepository

fun Routing.createPasswordAuthRoutes() {
    val repository: UserRepository by inject()

    post("/login") {
        with(call) {
            val req: LoginRequest = receive()
            respond(req)
        }
    }

    get("/login2") {
        with(call) {
            respond(LoginRequest(
                request.requiredQueryParam("email"),
                request.requiredQueryParam("password"),
            ))
        }
    }
}
