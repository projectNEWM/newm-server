package io.newm.server.logging

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import org.slf4j.event.Level

fun Application.installCallLogging() {
    install(CallLogging) {
        level = Level.DEBUG
    }
}
