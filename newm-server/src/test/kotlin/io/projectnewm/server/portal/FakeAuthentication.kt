package io.projectnewm.server.portal

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.AuthenticationProvider

fun Application.configureFakeAuthentication() {
    install(Authentication) {
        AuthenticationProvider(object : AuthenticationProvider.Configuration("auth-jwt") {}).apply {
            register(this)
        }
    }
}
