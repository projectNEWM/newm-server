package io.newm.server.features.user.oauth.providers

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.server.application.ApplicationEnvironment
import io.newm.server.auth.oauth.model.OAuthTokens
import io.newm.server.features.user.oauth.OAuthUser
import io.newm.server.features.user.oauth.OAuthUserProvider
import io.newm.server.ktx.checkedBody
import io.newm.shared.exception.HttpBadRequestException
import io.newm.shared.ktx.getConfigString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class GoogleUser(
    @SerialName("id")
    override val id: String,
    @SerialName("given_name")
    override val firstName: String? = null,
    @SerialName("family_name")
    override val lastName: String? = null,
    @SerialName("picture")
    override val pictureUrl: String? = null,
    @SerialName("email")
    override val email: String? = null,
    @SerialName("email_verified")
    override val isEmailVerified: Boolean? = null
) : OAuthUser

internal class GoogleUserProvider(
    environment: ApplicationEnvironment,
    private val httpClient: HttpClient
) : OAuthUserProvider {

    private val userInfoUrl = environment.getConfigString("oauth.google.userInfoUrl")

    override suspend fun getUser(tokens: OAuthTokens): OAuthUser {
        val token = tokens.accessToken ?: throw HttpBadRequestException("Google OAuth requires accessToken")
        return httpClient.get(userInfoUrl) {
            parameter(
                key = "fields",
                value = "id,given_name,family_name,picture,email"
            )
            headers {
                accept(ContentType.Application.Json)
                bearerAuth(token)
            }
        }.checkedBody<GoogleUser>()
    }
}
