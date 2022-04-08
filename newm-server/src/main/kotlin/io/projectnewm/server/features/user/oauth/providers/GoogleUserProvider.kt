package io.projectnewm.server.features.user.oauth.providers

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.projectnewm.server.features.user.oauth.OAuthUser
import io.projectnewm.server.features.user.oauth.OAuthUserProvider
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val ENDPOINT_URL = "https://www.googleapis.com/oauth2/v2/userinfo"

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
    override val email: String? = null
) : OAuthUser

internal class GoogleUserProvider(private val httpClient: HttpClient) : OAuthUserProvider {
    override suspend fun getUser(token: String): OAuthUser = httpClient.get(ENDPOINT_URL) {
        parameter(
            key = "fields",
            value = "id,given_name,family_name,picture,email"
        )
        headers {
            accept(ContentType.Application.Json)
            bearerAuth(token)
        }
    }.body<GoogleUser>()
}
