package io.projectnewm.server.auth

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.projectnewm.server.auth.jwt.configureJwt

fun Application.installAuthentication() {
    install(Authentication) {
        configureJwt()
    }
}
