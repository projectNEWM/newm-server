package io.newm.server.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import org.koin.dsl.module
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

val clientKoinModule = module {
    single {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(json = get())
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 2.minutes.inWholeMilliseconds
                connectTimeoutMillis = 10.seconds.inWholeMilliseconds
                socketTimeoutMillis = 30.seconds.inWholeMilliseconds
            }
        }
    }
}
