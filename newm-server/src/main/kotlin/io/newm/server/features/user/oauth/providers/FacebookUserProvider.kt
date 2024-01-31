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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class FacebookUser(
    @SerialName("id")
    override val id: String,
    @SerialName("first_name")
    override val firstName: String? = null,
    @SerialName("last_name")
    override val lastName: String? = null,
    @SerialName("email")
    override val email: String? = null,
    override var pictureUrl: String? = null
) : OAuthUser {
    override val isEmailVerified: Boolean
        get() = !email.isNullOrBlank() // Facebook API returns email only if verified
}

@Serializable
private data class Picture(
    @SerialName("data")
    val data: Data?
) {
    @Serializable
    data class Data(
        @SerialName("url")
        val url: String?
    )
}

internal class FacebookUserProvider(
    environment: ApplicationEnvironment,
    private val httpClient: HttpClient
) : OAuthUserProvider {
    private val userInfoUrl = environment.getConfigString("oauth.facebook.userInfoUrl")

    override suspend fun getUser(tokens: OAuthTokens): OAuthUser =
        coroutineScope {
            val token = tokens.accessToken ?: throw HttpBadRequestException("Facebook OAuth requires accessToken")
            val pictureJob =
                async {
                    httpClient.get("$userInfoUrl/picture") {
                        parameter(key = "type", value = "large")
                        parameter(key = "redirect", value = false)
                        headers {
                            accept(ContentType.Application.Json)
                            bearerAuth(token)
                        }
                    }.checkedBody<Picture>()
                }
            val userJob =
                async {
                    httpClient.get(userInfoUrl) {
                        parameter(
                            key = "fields",
                            value = "id,first_name,last_name,email"
                        )
                        headers {
                            accept(ContentType.Application.Json)
                            bearerAuth(token)
                        }
                    }.checkedBody<FacebookUser>()
                }
            userJob.await().apply {
                pictureUrl = pictureJob.await().data?.url.orEmpty()
            }
        }
}
