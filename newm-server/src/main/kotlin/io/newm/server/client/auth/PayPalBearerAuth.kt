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

fun AuthConfig.payPalBearer() =
    bearer {
        val loader = PayPalTokenLoader()

        // Load and refresh tokens without waiting for a 401 first if the host matches
        sendWithoutRequest { request ->
            request.url.host.endsWith(".paypal.com")
        }
        loadTokens {
            loader.tokens ?: loader.load()
        }
        refreshTokens {
            loader.load()
        }
    }

private class PayPalTokenLoader {
    private val environment: ApplicationEnvironment by inject()
    private val httpClient: HttpClient by inject()
    var tokens: BearerTokens? = null

    suspend fun load(): BearerTokens {
        val clientId = environment.getSecureConfigString("oauth.payPal.clientId")
        val clientSecret = environment.getSecureConfigString("oauth.payPal.clientSecret")
        val accessTokenUrl = environment.getConfigString("oauth.payPal.accessTokenUrl")
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
