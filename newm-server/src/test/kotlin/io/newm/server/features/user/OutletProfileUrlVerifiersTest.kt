package io.newm.server.features.user

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.google.common.truth.Truth.assertThat
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.newm.server.BaseApplicationTests
import io.newm.server.client.auth.appleMusicBearer
import io.newm.server.client.auth.soundCloudBearer
import io.newm.server.client.auth.spotifyBearer
import io.newm.server.features.user.verify.AppleMusicProfileUrlVerifier
import io.newm.server.features.user.verify.OutletProfileUrlVerificationException
import io.newm.server.features.user.verify.SoundCloudProfileUrlVerifier
import io.newm.server.features.user.verify.SpotifyProfileUrlVerifier
import io.newm.server.security.KeyParser
import io.newm.shared.serialization.BigDecimalSerializer
import io.newm.shared.serialization.BigIntegerSerializer
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.jsoup.Jsoup
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.math.BigInteger
import java.security.KeyFactory
import java.security.interfaces.ECKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class OutletProfileUrlVerifiersTest : BaseApplicationTests() {

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(
                json = Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                    isLenient = true
                    serializersModule = SerializersModule {
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
    }

    private val spotifyHttpClient: HttpClient by lazy {
        httpClient.config {
            install(Auth) {
                spotifyBearer()
            }
        }
    }

    private val appleMusicHttpClient: HttpClient by lazy {
        httpClient.config {
            install(Auth) {
                appleMusicBearer()
            }
        }
    }

    private val soundCloudHttpClient: HttpClient by lazy {
        httpClient.config {
            install(Auth) {
                soundCloudBearer()
            }
        }
    }

    @Test
    @Disabled("This test is disabled because it requires a valid Spotify API token")
    fun testSpotifyProfileUrlVerifierSuccess() = runBlocking {
        val verifier = SpotifyProfileUrlVerifier(spotifyHttpClient)
        verifier.verify("https://open.spotify.com/artist/3WrFJ7ztbogyGnTHbHJFl2", "The Beatles")
    }

    @Test
    @Disabled("This test is disabled because it requires a valid Spotify API token")
    fun testSpotifyProfileUrlVerifierFailure() = runBlocking {
        val verifier = SpotifyProfileUrlVerifier(spotifyHttpClient)
        val exception = assertThrows<OutletProfileUrlVerificationException> {
            verifier.verify("https://open.spotify.com/artist/3WrFJ7ztbogyGnTHbHJFl2", "Teh Beatles")
        }
        assertThat(exception.message).isEqualTo("Spotify profile name (The Beatles) does not match stageOrFullName (Teh Beatles) for 3WrFJ7ztbogyGnTHbHJFl2")
    }

    @Test
    @Disabled("This test is disabled because it requires a valid Apple Music JWT token")
    fun testAppleMusicProfileUrlVerifierSuccess() = runBlocking {
        val verifier = AppleMusicProfileUrlVerifier(appleMusicHttpClient)
        verifier.verify("https://music.apple.com/us/artist/beatles/136975", "The Beatles")
    }

    @Test
    @Disabled("This test is disabled because it requires a valid Apple Music JWT token")
    fun testAppleMusicProfileUrlVerifierFailure() = runBlocking {
        val verifier = AppleMusicProfileUrlVerifier(appleMusicHttpClient)
        val exception = assertThrows<OutletProfileUrlVerificationException> {
            verifier.verify("https://music.apple.com/us/artist/beatles/136975", "Teh Beatles")
        }
        assertThat(exception.message).isEqualTo("Apple Music profile name (The Beatles) does not match stageOrFullName (Teh Beatles) for 136975")
    }

    @Test
    @Disabled("This test is disabled because it requires a valid SoundCloud API token")
    fun testSoundCloudProfileUrlVerifierSuccess() = runBlocking {
        val verifier = SoundCloudProfileUrlVerifier(soundCloudHttpClient)
        verifier.verify("https://soundcloud.com/miraimusics", "Mirai")
    }

    @Test
    @Disabled("This test is disabled because it requires a valid SoundCloud API token")
    fun testSoundCloudProfileUrlVerifierFailure() = runBlocking {
        val verifier = SoundCloudProfileUrlVerifier(soundCloudHttpClient)
        val exception = assertThrows<OutletProfileUrlVerificationException> {
            verifier.verify("https://soundcloud.com/bogusprofile123", "Cringe Noize")
        }
        assertThat(exception.message).isEqualTo("SoundCloud profile not found for https://soundcloud.com/bogusprofile123")
    }

    @Test
    @Disabled("This test is disabled because it requires a valid AppleMusicKit private key")
    fun testCreateAppleMusicKitJWT() = runBlocking {
        val appleTeamId = "<apple_team_id>"
        val appleKeyId = "<apple_key_id>"
        // JWT with ES256 algorithm for MusicKit
        val privateKeyPEM = """
            -----BEGIN PRIVATE KEY-----
            <private_key_info>
            -----END PRIVATE KEY-----
        """.trimIndent()
        val keyBytes = KeyParser.parse(privateKeyPEM)
        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        val privateKey: ECKey = KeyFactory.getInstance("EC").generatePrivate(keySpec) as ECKey
        val algorithm = Algorithm.ECDSA256(privateKey)
        val token = JWT.create()
            .withKeyId(appleKeyId)
            .withIssuer(appleTeamId)
            .withExpiresAt(Date.from(Instant.now().plus(15777000L, ChronoUnit.SECONDS))) // 6 months
            .withIssuedAt(Date.from(Instant.now()))
            .sign(algorithm)
        println("JWT: $token")
        val decodedJWT = JWT.decode(token)
        println("Decoded JWT: $decodedJWT")
        if (decodedJWT.expiresAt.toInstant().isBefore(Instant.now().minus(1L, ChronoUnit.DAYS))) {
            println("Less than 1 day to expiry!!")
        } else {
            println("more than 1 day to expiry.")
        }
    }

    @Test
    @Disabled
    fun testJSoupSoundCloudUserId() = runBlocking {
        val doc = Jsoup.connect("https://soundcloud.com/miraimusics").get()
        val userId = doc.select("meta[property='twitter:app:url:iphone']").attr("content").substringAfterLast(':')
        assertThat(userId).isEqualTo("49651945")
    }
}
