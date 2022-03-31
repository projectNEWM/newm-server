package io.projectnewm.server.auth.oauth.repo

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.http.Parameters
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.util.logging.Logger
import io.projectnewm.server.auth.oauth.OAuthRepository
import io.projectnewm.server.auth.oauth.OAuthType
import io.projectnewm.server.ext.getConfigString
import kotlinx.serialization.Serializable
import org.slf4j.MarkerFactory

@Serializable
private data class TokenResponse(val accessToken: String)

class OAuthRepositoryImpl(
    private val environment: ApplicationEnvironment,
    private val httpClient: HttpClient,
    private val logger: Logger
) : OAuthRepository {
    private val marker = MarkerFactory.getMarker(javaClass.simpleName)

    override suspend fun getAccessToken(type: OAuthType, code: String, redirectUri: String): String {
        logger.debug(marker, "getAccessToken: type = $type, redirectUri=$redirectUri")

        val pathPrefix = "oauth.${type.name.lowercase()}"
        return httpClient.submitForm(
            url = environment.getConfigString("$pathPrefix.accessTokenUrl"),
            formParameters = Parameters.build {
                append("grant_type", "authorization_code")
                append("code", code)
                append("redirect_uri", redirectUri)
                append("client_id", environment.getConfigString("$pathPrefix.clientId"))
                append("client_secret", environment.getConfigString("$pathPrefix.clientSecret"))
            }
        ).body<TokenResponse>().accessToken
    }
}
