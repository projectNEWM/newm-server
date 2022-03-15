package io.projectnewm.server.auth.twofactor

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.projectnewm.server.ext.requiredQueryParam
import io.projectnewm.server.koin.inject

fun Routing.createTwoFactorAuthRoutes() {
    val repository: TwoFactorAuthRepository by inject()

    get("/auth/code") {
        repository.sendCode(call.request.requiredQueryParam("email"))
        call.respond(HttpStatusCode.NoContent)
    }
}
