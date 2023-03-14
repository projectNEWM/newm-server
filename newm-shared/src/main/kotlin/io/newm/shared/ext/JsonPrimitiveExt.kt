package io.newm.shared.ext

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.longOrNull

val JsonPrimitive.value: Any
    get() = if (isString) content else booleanOrNull ?: longOrNull ?: double
