package io.projectnewm.server.exception

import io.ktor.http.HttpStatusCode
import java.lang.Exception

sealed class HttpStatusException(val statusCode: HttpStatusCode, message: String) : Exception(message)

class HttpBadRequestException(message: String) : HttpStatusException(HttpStatusCode.BadRequest, message)

class HttpUnauthorizedException(message: String) : HttpStatusException(HttpStatusCode.Unauthorized, message)

class HttpNotFoundException(message: String) : HttpStatusException(HttpStatusCode.NotFound, message)

class HttpForbiddenException(message: String) : HttpStatusException(HttpStatusCode.Forbidden, message)

class HttpUnprocessableEntityException(message: String) : HttpStatusException(HttpStatusCode.UnprocessableEntity, message)

class HttpConflictException(message: String) : HttpStatusException(HttpStatusCode.Conflict, message)
