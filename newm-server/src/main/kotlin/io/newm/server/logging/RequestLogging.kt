package io.newm.server.logging

import io.newm.shared.koin.inject
import io.newm.shared.ktx.info
import kotlinx.serialization.json.Json
import org.slf4j.Logger

val json: Json by inject()

inline fun <reified T> T.logRequestJson(
    method: String,
    log: Logger
): T {
    log.info { "Sending ${method.uppercase()} Request ${this!!::class.simpleName}: ${json.encodeToString(this)}" }
    return this
}
