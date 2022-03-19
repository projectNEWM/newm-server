package io.projectnewm.server.cors

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.CORS
import io.projectnewm.server.ext.getConfigSplitStrings

fun Application.installCORS() {
    install(CORS) {
        method(HttpMethod.Put)
        method(HttpMethod.Patch)
        method(HttpMethod.Delete)
        header(HttpHeaders.Authorization)
        allowCredentials = true
        allowNonSimpleContentTypes = true

        environment.getConfigSplitStrings("cors.hosts").forEach { host ->
            val parts = host.split("://")
            if (parts.size > 1) {
                host(parts[1], listOf(parts[0]))
            } else {
                host(host)
            }
        }
        log.debug("CORS allowed hosts: $hosts")
    }
}
