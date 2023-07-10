package io.newm.server.logging

import io.ktor.server.application.Application
import io.newm.shared.ktx.getConfigString
import io.sentry.Sentry

fun Application.initializeSentry() {
    Sentry.init { options ->
        options.dsn = environment.getConfigString("sentry.dsn")
//        options.tracesSampleRate = if (environment.developmentMode) 1.0 else 0.5
        options.tracesSampleRate = 1.0
        options.isDebug = environment.developmentMode
        options.environment = environment.config.property("sentry.environment").getString()
    }
}

internal fun Throwable.captureToSentry() {
    Sentry.captureException(this)
}
