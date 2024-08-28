package io.newm.server.features.clientconfig

import io.ktor.http.ContentType
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route
import io.newm.server.config.repo.ConfigRepository
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_CLIENT_CONFIG_MARKETPLACE
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_CLIENT_CONFIG_MOBILE
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_CLIENT_CONFIG_STUDIO
import io.newm.server.recaptcha.repo.RecaptchaRepository
import io.newm.shared.ktx.get
import org.koin.ktor.ext.inject

private const val ROOT_PATH = "v1/client-config"

fun Routing.createClientConfigRoutes() {
    val configRepository: ConfigRepository by inject()
    val recaptchaRepository: RecaptchaRepository by inject()

    route(ROOT_PATH) {
        get("studio") {
            recaptchaRepository.verify("studio_config", request)
            respondText(
                text = configRepository.getString(CONFIG_KEY_CLIENT_CONFIG_STUDIO),
                contentType = ContentType.Application.Json
            )
        }
        get("marketplace") {
            recaptchaRepository.verify("marketplace_config", request)
            respondText(
                text = configRepository.getString(CONFIG_KEY_CLIENT_CONFIG_MARKETPLACE),
                contentType = ContentType.Application.Json
            )
        }
        get("mobile") {
            recaptchaRepository.verify("mobile_config", request)
            respondText(
                text = configRepository.getString(CONFIG_KEY_CLIENT_CONFIG_MOBILE),
                contentType = ContentType.Application.Json
            )
        }
    }
}
