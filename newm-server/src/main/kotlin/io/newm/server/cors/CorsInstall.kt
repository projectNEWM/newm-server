package io.newm.server.cors

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.cors.routing.CORS
import io.newm.server.recaptcha.RecaptchaHeaders
import io.newm.shared.ktx.getConfigSplitStrings

fun Application.installCORS() {
    install(CORS) {
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)

        // force POST to be allowed even though it is assumed to be a default
        this.methods.add(HttpMethod.Post)

        allowHeader(HttpHeaders.Authorization)
        allowHeader(RecaptchaHeaders.Platform)
        allowHeader(RecaptchaHeaders.Token)
        allowCredentials = true
        allowNonSimpleContentTypes = true

        this@installCORS.environment.getConfigSplitStrings("cors.hosts").forEach { host ->
            val parts = host.split("://")
            if (parts.size > 1) {
                allowHost(parts[1], listOf(parts[0]))
            } else {
                allowHost(host)
            }
        }
        this@installCORS.log.info("CORS allowed hosts: $hosts")
    }
}
