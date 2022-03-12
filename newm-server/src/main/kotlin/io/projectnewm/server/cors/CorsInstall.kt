package io.projectnewm.server.cors

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.CORS

fun Application.installCORS() {
    install(CORS) {
        anyHost()
    }
}
