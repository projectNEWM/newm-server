package io.newm.server.features.user.verify

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.request.accept
import io.ktor.client.request.basicAuth
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.util.logging.Logger
import io.newm.server.features.user.model.TokenInfo
import io.newm.server.ktx.getSecureConfigString
import io.newm.shared.koin.inject
import io.newm.shared.ktx.getConfigString
import io.newm.shared.ktx.info
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.core.parameter.parametersOf

class SpotifyProfileUrlVerifier(
    private val applicationEnvironment: ApplicationEnvironment,
    private val httpClient: HttpClient,
) : OutletProfileUrlVerifier {
    private val logger: Logger by inject { parametersOf(javaClass.simpleName) }

    private var bearerTokens: BearerTokens? = null
    private lateinit var authorizedHttpClient: HttpClient

    override suspend fun verify(outletProfileUrl: String, stageOrFullName: String) {
        authorizeClient()
        val spotifyProfileId = outletProfileUrl.substringAfterLast("/").substringBefore("?")
        val response = authorizedHttpClient.get(
            "https://api.spotify.com/v1/artists/$spotifyProfileId"
        ) {
            accept(ContentType.Application.Json)
        }
        if (!response.status.isSuccess()) {
            throw IllegalArgumentException("Spotify profile not found for $spotifyProfileId")
        }
        val spotifyArtistResponse: SpotifyArtistResponse = response.body()
        logger.info { "Spotify profile response for $spotifyProfileId : $spotifyArtistResponse" }
        if (spotifyArtistResponse.name != stageOrFullName) {
            throw IllegalArgumentException("Spotify profile name (${spotifyArtistResponse.name}) does not match stageOrFullName ($stageOrFullName) for $spotifyProfileId")
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
                                request.url.host == "api.spotify.com"
                            }
                            loadTokens {
                                bearerTokens ?: refreshBearerTokens()
                            }
                            refreshTokens {
                                refreshBearerTokens()
                            }
                        }
                    }
                }
        }
    }

    private suspend fun refreshBearerTokens(): BearerTokens {
        val clientId = applicationEnvironment.getSecureConfigString("oauth.spotify.clientId")
        val clientSecret = applicationEnvironment.getSecureConfigString("oauth.spotify.clientSecret")
        val accessTokenUrl = applicationEnvironment.getConfigString("oauth.spotify.accessTokenUrl")
        val tokenInfo: TokenInfo = httpClient.submitForm(
            url = accessTokenUrl,
            formParameters = parameters {
                append("grant_type", "client_credentials")
            }
        ) {
            basicAuth(clientId, clientSecret)
            accept(ContentType.Application.Json)
        }.body()
        bearerTokens = BearerTokens(tokenInfo.accessToken, tokenInfo.accessToken)
        return bearerTokens!!
    }

    @Serializable
    private data class SpotifyArtistResponse(
        @SerialName("id")
        val id: String,
        @SerialName("name")
        val name: String,
    )
}
