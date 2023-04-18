package io.newm.server.ktx

import io.newm.shared.exception.HttpBadRequestException

fun <T> T?.asMandatoryField(name: String): T {
    if (this == null) throw HttpBadRequestException("Missing required field: $name")
    return this
}
