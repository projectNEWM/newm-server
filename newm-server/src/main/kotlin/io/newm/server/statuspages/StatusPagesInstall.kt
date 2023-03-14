package io.newm.server.statuspages

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.newm.shared.exception.HttpStatusException
import io.newm.server.logging.captureToSentry
import org.jetbrains.exposed.dao.exceptions.EntityNotFoundException
import org.jetbrains.exposed.exceptions.ExposedSQLException

fun Application.installStatusPages() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            when (cause) {
                is HttpStatusException -> call.respondStatus(cause.statusCode, cause)
                is EntityNotFoundException -> call.respondStatus(HttpStatusCode.NotFound, cause)
                is IllegalArgumentException,
                is ExposedSQLException -> call.respondStatus(HttpStatusCode.UnprocessableEntity, cause)
                else -> {
                    call.respondStatus(HttpStatusCode.InternalServerError, cause)
                    cause.captureToSentry()
                }
            }
        }
    }
}

private suspend fun ApplicationCall.respondStatus(
    statusCode: HttpStatusCode,
    cause: Throwable
) = respond(
    status = statusCode,
    message = StatusResponse(
        code = statusCode.value,
        description = statusCode.description,
        cause = cause.message ?: cause.toString()
    )
)
