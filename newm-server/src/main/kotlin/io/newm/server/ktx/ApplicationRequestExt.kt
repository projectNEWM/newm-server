package io.newm.server.ktx

import io.ktor.server.request.ApplicationRequest
import io.newm.shared.exception.HttpBadRequestException

fun ApplicationRequest.requiredQueryParam(name: String): String =
    queryParameters[name] ?: throw HttpBadRequestException("Missing query param: $name")
