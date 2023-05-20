package io.newm.server.ktx

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.newm.shared.exception.HttpStatusException.Companion.toException

suspend inline fun <reified T> HttpResponse.checkedBody(): T {
    if (!status.isSuccess()) throw status.toException(bodyAsText())
    return body()
}
