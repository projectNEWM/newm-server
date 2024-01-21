package io.newm.server.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.newm.server.client.auth.appleMusicBearer
import io.newm.server.client.auth.soundCloudBearer
import io.newm.server.client.auth.spotifyBearer
import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

val QUALIFIER_SPOTIFY_HTTP_CLIENT = named("spotifyHttpClient")
val QUALIFIER_APPLE_MUSIC_HTTP_CLIENT = named("appleMusicHttpClient")
val QUALIFIER_SOUND_CLOUD_HTTP_CLIENT = named("soundCloudHttpClient")

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
    single(QUALIFIER_SPOTIFY_HTTP_CLIENT) {
        get<HttpClient>().config {
            install(Auth) {
                spotifyBearer()
            }
        }
    }
    single(QUALIFIER_APPLE_MUSIC_HTTP_CLIENT) {
        get<HttpClient>().config {
            install(Auth) {
                appleMusicBearer()
            }
        }
    }
    single(QUALIFIER_SOUND_CLOUD_HTTP_CLIENT) {
        get<HttpClient>().config {
            install(Auth) {
                soundCloudBearer()
            }
        }
    }
}
