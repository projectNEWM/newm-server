package io.newm.server.features.distribution.eveara

import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.server.application.ApplicationEnvironment
import io.newm.server.features.distribution.DistributionRepository
import io.newm.server.features.distribution.model.GetGenresResponse
import io.newm.server.features.song.database.SongEntity
import io.newm.server.ktx.getSecureConfigString
import io.newm.shared.koin.inject
import io.newm.shared.ktx.getConfigString
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Duration

class EvearaDistributionRepositoryImpl : DistributionRepository {
    private val httpClient: HttpClient by inject()
    private val applicationEnvironment: ApplicationEnvironment by inject()
    private val evearaServer by lazy { applicationEnvironment.getConfigString("eveara.server") }
    private suspend fun evearaClientId() = applicationEnvironment.getSecureConfigString("eveara.clientId")
    private suspend fun evearaClientSecret() = applicationEnvironment.getSecureConfigString("eveara.clientSecret")
    private val getTokenMutex = Mutex()
    private val apiTokenCache =
        Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(1L))
            .build<Int, ApiToken>()

    private suspend fun getEvearaApiToken(): String {
        getTokenMutex.withLock {
            return apiTokenCache.getIfPresent(-1)?.let { apiToken ->
                if (System.currentTimeMillis() > apiToken.expiryTimestampMillis - FIVE_MINUTES_IN_MILLIS) {
                    // Get an access token since it's close to expiry
                    null
                } else {
                    apiToken.accessToken
                }
            } ?: run {
                val response = httpClient.post("https://$evearaServer/api/v2.0/oauth/gettoken") {
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    setBody(
                        EvearaApiGetTokenRequest(
                            clientId = evearaClientId(),
                            clientSecret = evearaClientSecret(),
                        )
                    )
                }
                if (!response.status.isSuccess()) {
                    throw ServerResponseException(response, "Error getting Eveara accessToken!")
                }
                val apiTokenResponse: EvearaApiTokenResponse = response.body()
                if (!apiTokenResponse.success) {
                    throw ServerResponseException(response, "Error getting Eveara accessToken! success==false")
                }
                val apiToken = ApiToken(
                    accessToken = apiTokenResponse.accessToken,
                    refreshToken = apiTokenResponse.refreshToken,
                    expiryTimestampMillis = System.currentTimeMillis() + apiTokenResponse.expiresInSeconds * 1000L
                )
                apiTokenCache.put(-1, apiToken)

                apiToken.accessToken
            }
        }
    }

    override suspend fun getGenres(): GetGenresResponse {
        val response = httpClient.get("https://$evearaServer/api/v2.0/genres") {
            accept(ContentType.Application.Json)
            bearerAuth(getEvearaApiToken())
        }
        if (!response.status.isSuccess()) {
            throw ServerResponseException(response, "Error getting genres!")
        }
        val getGenresResponse: GetGenresResponse = response.body()
        if (!getGenresResponse.success) {
            throw ServerResponseException(response, "Error getting genres! success==false")
        }

        return getGenresResponse
    }

    override suspend fun distributeSong(song: SongEntity) {
        // TODO: Implement later. Focus on completing minting code first. Assume we have distributed to Eveara successfully.
    }

    private data class EvearaApiGetTokenRequest(
        @SerialName("grant_type")
        val grantType: String = "client_credentials",
        @SerialName("client_id")
        val clientId: String,
        @SerialName("client_secret")
        val clientSecret: String,
    )

    @Serializable
    private data class EvearaApiTokenResponse(
        @SerialName("success")
        val success: Boolean,
        @SerialName("access_token")
        val accessToken: String,
        @SerialName("refresh_token")
        val refreshToken: String,
        @SerialName("expires_in")
        val expiresInSeconds: Long,
    )

    private data class ApiToken(
        val accessToken: String,
        val refreshToken: String,
        val expiryTimestampMillis: Long,
    )

    companion object {
        private const val FIVE_MINUTES_IN_MILLIS = 300_000L
    }
}
