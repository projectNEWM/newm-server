package io.newm.server.content

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject

fun Application.installContentNegotiation() {
    val json: Json by inject()
    install(ContentNegotiation) {
        json(json = json)
    }
}
