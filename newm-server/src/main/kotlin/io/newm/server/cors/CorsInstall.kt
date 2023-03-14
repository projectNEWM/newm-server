package io.newm.server.cors

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.cors.routing.CORS
import io.newm.shared.ext.getConfigSplitStrings

fun Application.installCORS() {
    install(CORS) {
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.Authorization)
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
        this@installCORS.log.debug("CORS allowed hosts: $hosts")
    }
}
