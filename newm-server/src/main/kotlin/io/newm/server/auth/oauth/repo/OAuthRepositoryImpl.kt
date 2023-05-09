package io.newm.server.auth.oauth.repo

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.http.Parameters
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.util.logging.Logger
import io.newm.server.auth.oauth.model.OAuthTokens
import io.newm.server.auth.oauth.model.OAuthType
import io.newm.server.ktx.getSecureString
import io.newm.shared.koin.inject
import io.newm.shared.ktx.debug
import io.newm.shared.ktx.getConfigChild
import io.newm.shared.ktx.getString
import org.koin.core.parameter.parametersOf

class OAuthRepositoryImpl(
    private val environment: ApplicationEnvironment,
    private val httpClient: HttpClient,
) : OAuthRepository {
    private val logger: Logger by inject { parametersOf(javaClass.simpleName) }

    override suspend fun getTokens(type: OAuthType, code: String, redirectUri: String?): OAuthTokens {
        logger.debug { "getTokens: type = $type, redirectUri=$redirectUri" }

        return with(environment.getConfigChild("oauth.${type.name.lowercase()}")) {
            httpClient.submitForm(
                url = getString("accessTokenUrl"),
                formParameters = Parameters.build {
                    append("grant_type", "authorization_code")
                    append("code", code)
                    redirectUri?.let { append("redirect_uri", it) }
                    append("client_id", getSecureString("clientId"))
                    append("client_secret", getSecureString("clientSecret"))
                }
            ).body()
        }
    }
}
