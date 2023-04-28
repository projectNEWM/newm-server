package io.newm.server.features.user.oauth.providers

import com.auth0.jwt.JWT
import com.auth0.jwt.interfaces.DecodedJWT
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.http.Parameters
import io.ktor.server.application.ApplicationEnvironment
import io.newm.server.features.user.oauth.OAuthUser
import io.newm.server.features.user.oauth.OAuthUserProvider
import io.newm.server.ktx.getSecureString
import io.newm.shared.ktx.getConfigChild
import io.newm.shared.ktx.getString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class AppleUser(
    @SerialName("id_token")
    val idToken: String,
) : OAuthUser {

    private val jwt: DecodedJWT by lazy {
        JWT.decode(idToken)
    }
    override val id: String
        get() = jwt.subject!!

    override val firstName: String? = null
    override val lastName: String? = null
    override val pictureUrl: String? = null
    override val email: String?
        get() = jwt.getClaim("email").asString()
}

internal class AppleUserProvider(
    private val environment: ApplicationEnvironment,
    private val httpClient: HttpClient
) : OAuthUserProvider {

    override suspend fun getUser(token: String): OAuthUser = with(environment.getConfigChild("oauth.apple")) {
        httpClient.submitForm(
            url = getString("accessTokenUrl"),
            formParameters = Parameters.build {
                append("grant_type", "refresh_token")
                append("refresh_token", token)
                append("client_id", getSecureString("clientId"))
                append("client_secret", getSecureString("clientSecret"))
            }
        ).body<AppleUser>()
    }
}
