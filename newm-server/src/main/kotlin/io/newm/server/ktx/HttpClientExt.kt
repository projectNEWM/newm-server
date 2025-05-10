package io.newm.server.ktx

import io.ktor.client.HttpClient
import io.ktor.client.request.head
import io.ktor.client.statement.HttpResponse

suspend fun HttpClient.getFileSize(url: String): Long {
    val response: HttpResponse = head(url)
    return response.headers["Content-Length"]?.toLongOrNull() ?: 0L
}
