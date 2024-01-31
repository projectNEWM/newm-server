package io.newm.server.features.release.repo

import com.google.common.truth.Truth.assertThat
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.newm.server.BaseApplicationTests
import io.newm.server.client.auth.spotifyBearer
import io.newm.server.features.song.model.Song
import io.newm.server.features.song.repo.SongRepository
import io.newm.shared.serialization.BigDecimalSerializer
import io.newm.shared.serialization.BigIntegerSerializer
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.util.UUID
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class OutletReleaseRepositoryTest : BaseApplicationTests() {
    private val httpClient: HttpClient by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    json =
                        Json {
                            ignoreUnknownKeys = true
                            explicitNulls = false
                            isLenient = true
                            serializersModule =
                                SerializersModule {
                                    contextual(BigDecimal::class, BigDecimalSerializer)
                                    contextual(BigInteger::class, BigIntegerSerializer)
                                }
                        }
                )
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 2.minutes.inWholeMilliseconds
                connectTimeoutMillis = 10.seconds.inWholeMilliseconds
                socketTimeoutMillis = 30.seconds.inWholeMilliseconds
            }
            install(Auth) {
                spotifyBearer()
            }
        }
    }

    @Test
    @Disabled("This test is disabled because it requires a valid Spotify API token")
    fun testIsReleasedTrue() =
        runBlocking {
            val songId = UUID.randomUUID()
            val song =
                mockk<Song>(relaxed = true) {
                    every { isrc } returns "IE-WNY-23-492250" // Nido: After The Storm
                }
            val songRepository =
                mockk<SongRepository>(relaxed = true) {
                    coEvery { get(songId) } returns song
                }
            val releaseRepository = OutletReleaseRepositoryImpl(httpClient, songRepository)
            assertThat(releaseRepository.isSongReleased(songId)).isTrue()
        }

    @Test
    @Disabled("This test is disabled because it requires a valid Spotify API token")
    fun testIsReleasedFalse() =
        runBlocking {
            val songId = UUID.randomUUID()
            val song =
                mockk<Song>(relaxed = true) {
                    every { isrc } returns "IE-WNY-00-000000" // Garbage
                }
            val songRepository =
                mockk<SongRepository>(relaxed = true) {
                    coEvery { get(songId) } returns song
                }
            val releaseRepository = OutletReleaseRepositoryImpl(httpClient, songRepository)
            assertThat(releaseRepository.isSongReleased(songId)).isFalse()
        }
}
