package io.newm.ardrive.turbo.util

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode

class TurboHttpException(
    val url: String,
    val status: HttpStatusCode,
    val contentType: ContentType?,
    val responseBodySnippet: String,
) : RuntimeException(
        buildString {
            append("Turbo API request failed: ")
            append(status.value)
            append(' ')
            append(status.description)
            append(" (url=")
            append(url)
            append(")")
            if (contentType != null) {
                append(", contentType=")
                append(contentType)
            }
            if (responseBodySnippet.isNotBlank()) {
                append(", body=")
                append(responseBodySnippet)
            }
        },
    )
