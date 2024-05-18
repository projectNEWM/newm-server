package io.newm.server.model

import io.newm.shared.ktx.splitAndTrim
import io.newm.shared.ktx.toUUID
import java.util.UUID

data class FilterCriteria<T>(
    val includes: List<T>? = null,
    val excludes: List<T>? = null
)

fun <T> String.toFilterCriteria(transform: (String) -> T): FilterCriteria<T> {
    val includes = mutableListOf<T>()
    val excludes = mutableListOf<T>()
    for (str in splitAndTrim()) {
        when {
            str.startsWith('-') -> excludes += transform(str.substring(1))
            str.startsWith('+') -> includes += transform(str.substring(1))
            else -> includes += transform(str)
        }
    }
    return FilterCriteria(
        includes = includes.takeIf { it.isNotEmpty() },
        excludes = excludes.takeIf { it.isNotEmpty() }
    )
}

fun String.toStringFilterCriteria(): FilterCriteria<String> = toFilterCriteria { it }

fun String.toUUIDFilterCriteria(): FilterCriteria<UUID> = toFilterCriteria(String::toUUID)
