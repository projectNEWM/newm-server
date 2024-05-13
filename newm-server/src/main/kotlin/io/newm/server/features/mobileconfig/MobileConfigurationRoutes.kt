package io.newm.server.features.mobileconfig

import io.ktor.http.ContentType
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.newm.server.config.repo.ConfigRepository
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MOBILE_CONFIG_PAYLOAD
import io.newm.server.recaptcha.repo.RecaptchaRepository
import io.newm.shared.ktx.get
import org.koin.ktor.ext.inject

private const val ROOT_PATH = "v1/mobile-config"

fun Routing.createMobileConfigRoutes() {
    val configRepository: ConfigRepository by inject()
    val recaptchaRepository: RecaptchaRepository by inject()

    get(ROOT_PATH) {
        recaptchaRepository.verify("mobile_config", request)
        respondText(
            text = configRepository.getString(CONFIG_KEY_MOBILE_CONFIG_PAYLOAD),
            contentType = ContentType.Application.Json
        )
    }
}
