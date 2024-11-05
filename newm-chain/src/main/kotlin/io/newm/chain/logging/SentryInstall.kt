package io.newm.chain.logging

import io.ktor.server.application.Application
import io.sentry.Sentry

// FIXME: Extract this into a shared module
fun Application.initializeSentry() {
    Sentry.init { options ->
        options.dsn = environment.config.property("sentry.dsn").getString()
//        options.tracesSampleRate = if (environment.developmentMode) 1.0 else 0.5
        options.tracesSampleRate = 1.0
        options.isDebug = this.developmentMode
        options.environment = environment.config.property("sentry.environment").getString()
    }
}

internal fun Throwable.captureToSentry() {
    Sentry.captureException(this)
}
