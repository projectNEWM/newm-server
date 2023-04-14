package io.newm.server.auth.oauth.repo

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.http.Parameters
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.util.logging.Logger
import io.newm.server.auth.oauth.OAuthType
import io.newm.server.ktx.getSecureConfigString
import io.newm.shared.koin.inject
import io.newm.shared.ktx.debug
import io.newm.shared.ktx.getConfigString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.core.parameter.parametersOf

@Serializable
private data class TokenResponse(
    @SerialName("access_token")
    val accessToken: String
)

class OAuthRepositoryImpl(
    private val environment: ApplicationEnvironment,
    private val httpClient: HttpClient,
) : OAuthRepository {
    private val logger: Logger by inject { parametersOf(javaClass.simpleName) }

    override suspend fun getAccessToken(type: OAuthType, code: String, redirectUri: String): String {
        logger.debug { "getAccessToken: type = $type, redirectUri=$redirectUri" }

        val configPath = "oauth.${type.name.lowercase()}"
        return httpClient.submitForm(
            url = environment.getConfigString("$configPath.accessTokenUrl"),
            formParameters = Parameters.build {
                append("grant_type", "authorization_code")
                append("code", code)
                append("redirect_uri", redirectUri)
                append("client_id", environment.getSecureConfigString("$configPath.clientId"))
                append("client_secret", environment.getSecureConfigString("$configPath.clientSecret"))
            }
        ).body<TokenResponse>().accessToken
    }
}
