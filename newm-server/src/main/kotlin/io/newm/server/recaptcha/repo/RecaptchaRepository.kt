package io.newm.server.recaptcha.repo

import io.ktor.server.request.ApplicationRequest
import io.newm.shared.exception.HttpStatusException

interface RecaptchaRepository {
    @Throws(HttpStatusException::class)
    suspend fun verify(
        action: String,
        request: ApplicationRequest
    )
}
