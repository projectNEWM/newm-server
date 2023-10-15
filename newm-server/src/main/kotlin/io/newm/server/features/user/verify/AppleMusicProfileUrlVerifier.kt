package io.newm.server.features.user.verify

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.isSuccess
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.util.logging.Logger
import io.newm.server.ktx.getSecureConfigString
import io.newm.server.security.KeyParser
import io.newm.shared.koin.inject
import io.newm.shared.ktx.info
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.core.parameter.parametersOf
import java.security.KeyFactory
import java.security.interfaces.ECKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

class AppleMusicProfileUrlVerifier(
    private val applicationEnvironment: ApplicationEnvironment,
    private val httpClient: HttpClient,
) : OutletProfileUrlVerifier {
    private val logger: Logger by inject { parametersOf(javaClass.simpleName) }

    private var bearerTokens: BearerTokens? = null
    private lateinit var authorizedHttpClient: HttpClient

    override suspend fun verify(outletProfileUrl: String, stageOrFullName: String) {
        authorizeClient()
        val appleProfileId = outletProfileUrl.substringAfterLast("/")
        val response = authorizedHttpClient.get(
            "https://api.music.apple.com/v1/catalog/us/artists/$appleProfileId"
        ) {
            accept(ContentType.Application.Json)
        }
        if (!response.status.isSuccess()) {
            throw IllegalArgumentException("Apple Music profile not found for $appleProfileId")
        }
        val appleMusicArtistResponse: AppleMusicArtistResponse = response.body()
        if (appleMusicArtistResponse.data.isEmpty()) {
            throw IllegalArgumentException("Apple Music profile not found for $appleProfileId")
        }
        logger.info { "Apple Music profile response for $appleProfileId : $response" }
        if (appleMusicArtistResponse.data[0].attributes.name != stageOrFullName) {
            throw IllegalArgumentException("Apple Music profile name (${appleMusicArtistResponse.data[0].attributes.name}) does not match stageOrFullName ($stageOrFullName) for $appleProfileId")
        }
    }

    private suspend fun authorizeClient() {
        if (!::authorizedHttpClient.isInitialized) {
            authorizedHttpClient =
                httpClient.config {
                    install(Auth) {
                        bearer {
                            // Load and refresh tokens without waiting for a 401 first if the host matches
                            sendWithoutRequest { request ->
                                request.url.host == "api.music.apple.com"
                            }
                            loadTokens {
                                refreshedBearerTokens()
                            }
                            refreshTokens {
                                bearerTokens = null
                                refreshedBearerTokens()
                            }
                        }
                    }
                }
        }
    }

    private suspend fun refreshedBearerTokens(): BearerTokens {
        bearerTokens?.accessToken?.let { jwtToken ->
            val decodedToken = JWT.decode(jwtToken)
            if (decodedToken.expiresAt.toInstant().isAfter(Instant.now().plus(5L, ChronoUnit.MINUTES))) {
                // Token is still valid for over 5 minutes
                return bearerTokens!!
            }
        }

        val keyBytes = KeyParser.parse(applicationEnvironment.getSecureConfigString("jwt.apple.musickit.privateKey"))
        val teamId = applicationEnvironment.getSecureConfigString("jwt.apple.musickit.teamId")
        val keyId = applicationEnvironment.getSecureConfigString("jwt.apple.musickit.keyId")
        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        val privateKey: ECKey = KeyFactory.getInstance("EC").generatePrivate(keySpec) as ECKey
        val algorithm = Algorithm.ECDSA256(privateKey)
        val token = JWT.create()
            .withKeyId(keyId)
            .withIssuer(teamId)
            .withExpiresAt(Date.from(Instant.now().plus(1L, ChronoUnit.HOURS)))
            .withIssuedAt(Date.from(Instant.now()))
            .sign(algorithm)
        bearerTokens = BearerTokens(token, token)
        return bearerTokens!!
    }

    @Serializable
    private data class AppleMusicArtistResponse(
        @SerialName("data")
        val data: List<AppleMusicArtist>,
    )

    @Serializable
    private data class AppleMusicArtist(
        @SerialName("id")
        val id: String,
        @SerialName("attributes")
        val attributes: AppleMusicArtistAttributes,
    )

    @Serializable
    private data class AppleMusicArtistAttributes(
        @SerialName("name")
        val name: String,
    )
}
