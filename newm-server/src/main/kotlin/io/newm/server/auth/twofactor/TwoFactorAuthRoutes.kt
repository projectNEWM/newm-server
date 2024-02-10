package io.newm.server.auth.twofactor

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.newm.server.auth.AUTH_PATH
import io.newm.server.auth.twofactor.repo.TwoFactorAuthRepository
import io.newm.server.ktx.requiredQueryParam
import io.newm.server.recaptcha.repo.RecaptchaRepository
import io.newm.shared.koin.inject
import io.newm.shared.ktx.get

fun Routing.createTwoFactorAuthRoutes() {
    val recaptchaRepository: RecaptchaRepository by inject()
    val twoFactorAuthRepository: TwoFactorAuthRepository by inject()

    get("$AUTH_PATH/code") {
        recaptchaRepository.verify("auth_code", request)
        twoFactorAuthRepository.sendCode(request.requiredQueryParam("email"))
        respond(HttpStatusCode.NoContent)
    }
}
