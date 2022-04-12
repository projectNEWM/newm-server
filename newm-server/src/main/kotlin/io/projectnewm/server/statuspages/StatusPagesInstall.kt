package io.projectnewm.server.statuspages

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.projectnewm.server.exception.HttpStatusException
import io.projectnewm.server.logging.captureToSentry
import io.sentry.Sentry
import org.jetbrains.exposed.dao.exceptions.EntityNotFoundException

fun Application.installStatusPages() {
    install(StatusPages) {
        exception<HttpStatusException> { call, cause ->
            Sentry.addBreadcrumb(cause.message.orEmpty())
            call.respondStatus(cause.statusCode, cause)
        }

        exception<EntityNotFoundException> { call, cause ->
            Sentry.addBreadcrumb(cause.message.orEmpty())
            call.respondStatus(HttpStatusCode.NotFound, cause)
        }

        exception<Throwable> { call, cause ->
            call.respondStatus(HttpStatusCode.InternalServerError, cause)
            cause.captureToSentry()
        }
    }
}

private suspend fun ApplicationCall.respondStatus(
    statusCode: HttpStatusCode,
    cause: Throwable
) = respond(
    status = statusCode,
    message = StatusResponse(
        code = statusCode.value, description = statusCode.description, cause = cause.message ?: cause.toString()
    )
)
