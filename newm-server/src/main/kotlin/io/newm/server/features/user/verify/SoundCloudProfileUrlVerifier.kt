package io.newm.server.features.user.verify

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.request.accept
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.koin.core.parameter.parametersOf

class SoundCloudProfileUrlVerifier(
    private val applicationEnvironment: ApplicationEnvironment,
    private val httpClient: HttpClient,
) : OutletProfileUrlVerifier {
    private val logger: Logger by inject { parametersOf(javaClass.simpleName) }

    private var bearerTokens: BearerTokens? = null
    private lateinit var authorizedHttpClient: HttpClient

    override suspend fun verify(outletProfileUrl: String, stageOrFullName: String) {
        authorizeClient()
        val doc = withContext(Dispatchers.IO) {
            try {
                Jsoup.connect(outletProfileUrl).get()
            } catch (e: HttpStatusException) {
                throw IllegalArgumentException("SoundCloud profile not found for $outletProfileUrl")
            }
        }
        val userId = doc.select("meta[property='twitter:app:url:iphone']").attr("content").substringAfterLast(':')
        val response = authorizedHttpClient.get(
            "https://api.soundcloud.com/users/$userId"
        ) {
            accept(ContentType.Application.Json)
        }
        if (!response.status.isSuccess()) {
            throw IllegalArgumentException("SoundCloud profile not found for $userId")
        }
        val soundCloudArtistResponse: SoundCloudArtistResponse = response.body()
        logger.info { "SoundCloud profile response for $userId : $soundCloudArtistResponse" }
        // NOTE: Name matching not required for SoundCloud in Eveara
    }

    private suspend fun authorizeClient() {
        if (!::authorizedHttpClient.isInitialized) {
            authorizedHttpClient =
                httpClient.config {
                    install(Auth) {
                        bearer {
                            // Load and refresh tokens without waiting for a 401 first if the host matches
                            sendWithoutRequest { request ->
                                request.url.host == "api.soundcloud.com"
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
        val clientId = applicationEnvironment.getSecureConfigString("oauth.soundcloud.clientId")
        val clientSecret = applicationEnvironment.getSecureConfigString("oauth.soundcloud.clientSecret")
        val accessTokenUrl = applicationEnvironment.getConfigString("oauth.soundcloud.accessTokenUrl")
        val tokenInfo: TokenInfo = httpClient.submitForm(
            url = accessTokenUrl,
            formParameters = parameters {
                append("grant_type", "client_credentials")
                append("client_id", clientId)
                append("client_secret", clientSecret)
            }
        ) {
            accept(ContentType.Application.Json)
        }.body()
        bearerTokens = BearerTokens(tokenInfo.accessToken, tokenInfo.accessToken)
        return bearerTokens!!
    }

    @Serializable
    private data class SoundCloudArtistResponse(
        @SerialName("username")
        val username: String,
        @SerialName("first_name")
        val firstName: String,
        @SerialName("last_name")
        val lastName: String,
        @SerialName("full_name")
        val fullName: String,
    )
}
