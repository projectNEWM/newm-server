package io.newm.server.auth.oauth.repo

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.http.Parameters
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.util.logging.Logger
import io.newm.server.auth.oauth.OAuthType
import io.newm.server.ext.getConfigChild
import io.newm.server.ext.getString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.slf4j.MarkerFactory

@Serializable
private data class TokenResponse(
    @SerialName("access_token")
    val accessToken: String
)

class OAuthRepositoryImpl(
    private val environment: ApplicationEnvironment,
    private val httpClient: HttpClient,
    private val logger: Logger
) : OAuthRepository {
    private val marker = MarkerFactory.getMarker(javaClass.simpleName)

    override suspend fun getAccessToken(type: OAuthType, code: String, redirectUri: String): String {
        logger.debug(marker, "getAccessToken: type = $type, redirectUri=$redirectUri")

        val config = environment.getConfigChild("oauth.${type.name.lowercase()}")
        return httpClient.submitForm(
            url = config.getString("accessTokenUrl"),
            formParameters = Parameters.build {
                append("grant_type", "authorization_code")
                append("code", code)
                append("redirect_uri", redirectUri)
                append("client_id", config.getString("clientId"))
                append("client_secret", config.getString("clientSecret"))
            }
        ).body<TokenResponse>().accessToken
    }
}
