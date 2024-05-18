package io.newm.server.auth.oauth.repo

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.newm.server.auth.oauth.model.OAuthTokens
import io.newm.server.auth.oauth.model.OAuthType
import io.newm.server.ktx.checkedBody
import io.newm.server.ktx.getSecureString
import io.newm.shared.ktx.getConfigChild
import io.newm.shared.ktx.getString

class OAuthRepositoryImpl(
    private val environment: ApplicationEnvironment,
    private val httpClient: HttpClient,
) : OAuthRepository {
    private val logger = KotlinLogging.logger {}

    override suspend fun getTokens(
        type: OAuthType,
        code: String,
        redirectUri: String?
    ): OAuthTokens {
        logger.debug { "getTokens: type = $type, redirectUri=$redirectUri" }

        with(environment.getConfigChild("oauth.${type.name.lowercase()}")) {
            return httpClient.submitForm(
                url = getString("accessTokenUrl"),
                formParameters =
                    Parameters.build {
                        append("grant_type", "authorization_code")
                        append("code", code)
                        redirectUri?.let { append("redirect_uri", it) }
                        append("client_id", getSecureString("clientId"))
                        append("client_secret", getSecureString("clientSecret"))
                    }
            ).checkedBody()
        }
    }
}
