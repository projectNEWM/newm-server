package io.projectnewm.server.sessions

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie

fun Application.installSessions() {
    install(Sessions) {
        cookie<UserSession>("user-session")
        cookie<RedirectSession>("redirect-session")
    }
}
