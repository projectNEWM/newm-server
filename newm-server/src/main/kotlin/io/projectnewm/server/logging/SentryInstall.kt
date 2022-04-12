package io.projectnewm.server.logging

import io.ktor.server.application.*
import io.projectnewm.server.ext.getConfigString
import io.sentry.Sentry
import java.util.logging.Logger

fun Application.initializeSentry() {
    Sentry.init { options ->
        options.dsn = environment.getConfigString("sentry.dns")
//        options.tracesSampleRate = if (environment.developmentMode) 1.0 else 0.5
        options.tracesSampleRate = 1.0
        options.setDebug(environment.developmentMode)
    }

    Logger.getGlobal().warning("Initializing sentry")
    
    try {
        throw Exception("This is a test @@@")
    } catch (e: Exception) {
        Sentry.captureException(e)
    }
}

internal fun Throwable.captureToSentry() {
    Sentry.captureException(this)
}
