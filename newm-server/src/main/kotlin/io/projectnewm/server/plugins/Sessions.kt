package io.projectnewm.server.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.projectnewm.server.sessions.UserSession

fun Application.configureSessions() {
    install(Sessions) {
        cookie<UserSession>("user-session")
    }
}
