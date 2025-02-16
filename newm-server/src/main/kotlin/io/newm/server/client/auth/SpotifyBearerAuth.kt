package io.newm.server.client.auth

import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.AuthConfig
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.request.accept
import io.ktor.client.request.basicAuth
import io.ktor.client.request.forms.submitForm
import io.ktor.http.ContentType
import io.ktor.http.parameters
import io.ktor.server.application.ApplicationEnvironment
import io.newm.server.client.auth.model.TokenInfo
import io.newm.server.ktx.checkedBody
import io.newm.server.ktx.getSecureConfigString
import io.newm.shared.koin.inject
import io.newm.shared.ktx.getConfigString

fun AuthConfig.spotifyBearer() =
    bearer {
        val loader = SpotifyTokenLoader()

        // Load and refresh tokens without waiting for a 401 first if the host matches
        sendWithoutRequest { request ->
            request.url.host == "api.spotify.com"
        }
        loadTokens {
            loader.tokens ?: loader.load()
        }
        refreshTokens {
            loader.load()
        }
    }

private class SpotifyTokenLoader {
    private val environment: ApplicationEnvironment by inject()
    private val httpClient: HttpClient by inject()
    var tokens: BearerTokens? = null

    suspend fun load(): BearerTokens {
        val clientId = environment.getSecureConfigString("oauth.spotify.clientId")
        val clientSecret = environment.getSecureConfigString("oauth.spotify.clientSecret")
        val accessTokenUrl = environment.getConfigString("oauth.spotify.accessTokenUrl")
        val tokenInfo: TokenInfo =
            httpClient
                .submitForm(
                    url = accessTokenUrl,
                    formParameters =
                        parameters {
                            append("grant_type", "client_credentials")
                        }
                ) {
                    basicAuth(clientId, clientSecret)
                    accept(ContentType.Application.Json)
                }.checkedBody()
        return BearerTokens(tokenInfo.accessToken, tokenInfo.accessToken).also { tokens = it }
    }
}
