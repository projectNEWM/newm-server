package io.projectnewm.server.sessions

import io.ktor.server.sessions.CurrentSession
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.get
import io.ktor.server.sessions.set

data class RedirectSession(val url: String)

var CurrentSession.redirectUrl: String?
    get() = get<RedirectSession>()?.url
    set(value) = value?.let { set(RedirectSession(it)) } ?: clear<RedirectSession>()
