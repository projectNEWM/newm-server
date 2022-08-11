package io.newm.server.auth.twofactor

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.newm.server.auth.AUTH_PATH
import io.newm.server.auth.twofactor.repo.TwoFactorAuthRepository
import io.newm.server.di.inject
import io.newm.server.ext.requiredQueryParam

fun Routing.createTwoFactorAuthRoutes() {
    val repository: TwoFactorAuthRepository by inject()

    get("$AUTH_PATH/code") {
        repository.sendCode(call.request.requiredQueryParam("email"))
        call.respond(HttpStatusCode.NoContent)
    }
}
