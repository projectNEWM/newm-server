package io.projectnewm.server.logging

import io.ktor.server.application.Application
import io.projectnewm.server.ext.getConfigString
import io.sentry.Sentry

fun Application.initializeSentry() {
    Sentry.init { options ->
        options.dsn = environment.getConfigString("sentry.dns")
        options.tracesSampleRate = if (environment.developmentMode) 1.0 else 0.5
        options.setDebug(environment.developmentMode)
    }
}
