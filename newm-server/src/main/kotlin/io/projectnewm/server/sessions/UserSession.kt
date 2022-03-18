package io.projectnewm.server.sessions

import io.ktor.server.sessions.CurrentSession
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.get
import io.ktor.server.sessions.set

data class UserSession(val token: String)

var CurrentSession.token: String?
    get() = get<UserSession>()?.token
    set(value) = value?.let { set(UserSession(it)) } ?: clear<UserSession>()
