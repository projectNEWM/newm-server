package io.projectnewm.server.pugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.sessions.CurrentSession
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.cookie
import io.ktor.server.sessions.get
import io.ktor.server.sessions.set

data class UserSession(val token: String)

fun Application.configureSessions() {
    install(Sessions) {
        cookie<UserSession>("user-session")
    }
}

var CurrentSession.token: String?
    get() = get<UserSession>()?.token
    set(value) = set(UserSession(value!!))

fun CurrentSession.clear() = clear<UserSession>()
