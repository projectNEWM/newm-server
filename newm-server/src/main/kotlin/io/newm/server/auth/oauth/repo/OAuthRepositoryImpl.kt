package io.newm.server.auth.oauth.repo

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.http.Parameters
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.util.logging.Logger
import io.newm.server.auth.oauth.OAuthType
import io.newm.server.ktx.getSecureString
import io.newm.shared.koin.inject
import io.newm.shared.ktx.debug
import io.newm.shared.ktx.getConfigChild
import io.newm.shared.ktx.getString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.core.parameter.parametersOf

@Serializable
private data class TokenResponse(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("refresh_token")
    val refreshToken: String
)

class OAuthRepositoryImpl(
    private val environment: ApplicationEnvironment,
    private val httpClient: HttpClient,
) : OAuthRepository {
    private val logger: Logger by inject { parametersOf(javaClass.simpleName) }

    override suspend fun getAccessToken(type: OAuthType, code: String, redirectUri: String): String {
        logger.debug { "getAccessToken: type = $type, redirectUri=$redirectUri" }

        return with(environment.getConfigChild("oauth.${type.name.lowercase()}")) {
            val resp = httpClient.submitForm(
                url = getString("accessTokenUrl"),
                formParameters = Parameters.build {
                    append("grant_type", "authorization_code")
                    append("code", code)
                    append("redirect_uri", redirectUri)
                    append("client_id", getSecureString("clientId"))
                    append("client_secret", getSecureString("clientSecret"))
                }
            ).body<TokenResponse>()
            // currently for Apple, accessToken is useless and refreshToken is really what we want
            if (type == OAuthType.Apple) resp.refreshToken else resp.accessToken
        }
    }
}
