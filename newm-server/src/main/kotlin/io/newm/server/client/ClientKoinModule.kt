package io.newm.server.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.newm.server.client.auth.appleMusicBearer
import io.newm.server.client.auth.soundCloudBearer
import io.newm.server.client.auth.spotifyBearer
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import okhttp3.ConnectionPool
import okhttp3.Protocol
import org.koin.core.qualifier.named
import org.koin.dsl.module

val QUALIFIER_SPOTIFY_HTTP_CLIENT = named("spotifyHttpClient")
val QUALIFIER_APPLE_MUSIC_HTTP_CLIENT = named("appleMusicHttpClient")
val QUALIFIER_SOUND_CLOUD_HTTP_CLIENT = named("soundCloudHttpClient")
val QUALIFIER_UPLOAD_TRACK_HTTP_CLIENT = named("uploadTrackHttpClient")

val clientKoinModule =
    module {
        single {
            HttpClient(CIO) {
                install(ContentNegotiation) {
                    json(json = get())
                }
                install(HttpTimeout) {
                    requestTimeoutMillis = 5.minutes.inWholeMilliseconds
                    connectTimeoutMillis = 30.seconds.inWholeMilliseconds
                    socketTimeoutMillis = 2.minutes.inWholeMilliseconds
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
        single(QUALIFIER_UPLOAD_TRACK_HTTP_CLIENT) {
            HttpClient(OkHttp) {
                engine {
                    config {
                        // Set timeouts
                        readTimeout(30, TimeUnit.SECONDS)
                        writeTimeout(5, TimeUnit.MINUTES)
                        connectTimeout(30, TimeUnit.SECONDS)

                        // Enable connection pooling
                        connectionPool(
                            ConnectionPool(
                                maxIdleConnections = 10,
                                keepAliveDuration = 5,
                                timeUnit = TimeUnit.MINUTES,
                            )
                        )

                        // Configure client for large file transfers
                        protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
                        retryOnConnectionFailure(true)
                    }
                }
                install(ContentNegotiation) {
                    json(json = get())
                }
                install(HttpTimeout) {
                    requestTimeoutMillis = 5.minutes.inWholeMilliseconds
                    connectTimeoutMillis = 30.seconds.inWholeMilliseconds
                    socketTimeoutMillis = 3.minutes.inWholeMilliseconds
                }
            }
        }
    }
