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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val ENDPOINT_URL = "https://graph.facebook.com/v13.0/me"

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
) : OAuthUser

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

internal class FacebookUserProvider(private val httpClient: HttpClient) : OAuthUserProvider {
    override suspend fun getUser(token: String): OAuthUser = coroutineScope {
        val pictureJob = async {
            httpClient.get("$ENDPOINT_URL/picture") {
                parameter(key = "type", value = "large")
                parameter(key = "redirect", value = false)
                headers {
                    accept(ContentType.Application.Json)
                    bearerAuth(token)
                }
            }.body<Picture>()
        }
        val userJob = async {
            httpClient.get(ENDPOINT_URL) {
                parameter(
                    key = "fields",
                    value = "id,first_name,last_name,email"
                )
                headers {
                    accept(ContentType.Application.Json)
                    bearerAuth(token)
                }
            }.body<FacebookUser>()
        }
        userJob.await().apply {
            pictureUrl = pictureJob.await().data?.url.orEmpty()
        }
    }
}
