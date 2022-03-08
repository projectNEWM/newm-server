package io.projectnewm.server.auth

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.projectnewm.server.auth.jwt.configureJwt
import io.projectnewm.server.auth.oauth.configureOAuth
import io.projectnewm.server.oauth.OAuthType

fun Application.installAuthentication() {
    install(Authentication) {
        configureOAuth(OAuthType.Google)
        configureOAuth(OAuthType.Facebook)
        configureOAuth(OAuthType.LinkedIn)
        configureJwt()
    }
}
