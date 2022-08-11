package io.projectnewm.server.ext

import io.ktor.server.request.ApplicationRequest
import io.projectnewm.server.exception.HttpBadRequestException

fun ApplicationRequest.requiredQueryParam(name: String): String =
    queryParameters[name] ?: throw HttpBadRequestException("Missing query param: $name")
