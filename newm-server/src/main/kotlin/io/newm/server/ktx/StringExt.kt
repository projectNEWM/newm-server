package io.newm.server.ktx

import io.newm.shared.exception.HttpBadRequestException
import io.newm.shared.exception.HttpUnprocessableEntityException
import io.newm.shared.ktx.isValidEmail
import io.newm.shared.ktx.isValidUrl

fun String.checkLength(name: String, max: Int = 64) {
    if (length > max) throw HttpUnprocessableEntityException("Field $name exceeds $max chars limit")
}

fun String?.asValidEmail(): String {
    if (isNullOrBlank()) throw HttpBadRequestException("Missing email")
    if (!isValidEmail()) throw HttpUnprocessableEntityException("Invalid email: $this")
    return this
}

fun String?.asValidUrl(): String {
    if (isNullOrBlank()) throw HttpBadRequestException("Missing url")
    if (!isValidUrl()) throw HttpUnprocessableEntityException("Invalid url: $this")
    return this
}
