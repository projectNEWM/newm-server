package io.newm.shared.exception

import io.ktor.http.HttpStatusCode

sealed class HttpStatusException(val statusCode: HttpStatusCode, message: String) : Exception(message) {
    companion object {
        fun HttpStatusCode.toException(message: String): HttpStatusException {
            return when (this) {
                HttpStatusCode.BadRequest -> HttpBadRequestException(message)
                HttpStatusCode.Unauthorized -> HttpUnauthorizedException(message)
                HttpStatusCode.NotFound -> HttpUnauthorizedException(message)
                HttpStatusCode.Forbidden -> HttpUnauthorizedException(message)
                HttpStatusCode.UnprocessableEntity -> HttpUnauthorizedException(message)
                HttpStatusCode.Conflict -> HttpUnauthorizedException(message)
                else -> HttpUnknownException(this, message)
            }
        }
    }
}

class HttpBadRequestException(message: String) : HttpStatusException(HttpStatusCode.BadRequest, message)

class HttpUnauthorizedException(message: String) : HttpStatusException(HttpStatusCode.Unauthorized, message)

class HttpNotFoundException(message: String) : HttpStatusException(HttpStatusCode.NotFound, message)

class HttpForbiddenException(message: String) : HttpStatusException(HttpStatusCode.Forbidden, message)

class HttpUnprocessableEntityException(message: String) :
    HttpStatusException(HttpStatusCode.UnprocessableEntity, message)

class HttpConflictException(message: String) : HttpStatusException(HttpStatusCode.Conflict, message)

class HttpUnknownException(httpStatusCode: HttpStatusCode, message: String) :
    HttpStatusException(httpStatusCode, message)
