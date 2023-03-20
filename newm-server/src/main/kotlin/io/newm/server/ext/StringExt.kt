package io.newm.server.ext

import io.newm.shared.exception.HttpUnprocessableEntityException

fun String.checkLength(name: String, max: Int = 64) {
    if (length > max) throw HttpUnprocessableEntityException("field $name exceeds $max chars limit")
}
