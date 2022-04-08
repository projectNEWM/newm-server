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

private const val USER_ENDPOINT_URL = "https://api.linkedin.com/v2/me"
private const val EMAIL_ENDPOINT_URL = "https://api.linkedin.com/v2/clientAwareMemberHandles"

@Serializable
private data class LinkedInUser(
    @SerialName("id")
    override val id: String,
    @SerialName("localizedFirstName")
    override val firstName: String? = null,
    @SerialName("localizedLastName")
    override val lastName: String? = null,
    @SerialName("profilePicture")
    val picture: Picture? = null
) : OAuthUser {

    override val pictureUrl: String
        get() = picture?.image?.elements?.firstOrNull()?.identifiers?.firstOrNull()?.identifier.orEmpty()

    override var email: String? = null

    @Serializable
    data class Picture(
        @SerialName("displayImage~")
        val image: Image?
    ) {
        @Serializable
        data class Image(
            @SerialName("elements")
            val elements: List<Element>?
        ) {
            @Serializable
            data class Element(
                @SerialName("identifiers")
                val identifiers: List<Identifier>?
            ) {
                @Serializable
                data class Identifier(
                    @SerialName("identifier")
                    val identifier: String?
                )
            }
        }
    }
}

@Serializable
private data class EmailHandle(
    @SerialName("elements")
    val elements: List<Element>?
) {
    @Serializable
    data class Element(
        @SerialName("handle~")
        val handle: Handle?,
        @SerialName("primary")
        val primary: Boolean?
    ) {
        @Serializable
        data class Handle(
            @SerialName("emailAddress")
            val emailAddress: String?
        )

        val email: String?
            get() = handle?.emailAddress
    }

    val bestEmail: String?
        get() = elements?.firstOrNull { it.primary == true }?.email ?: elements?.firstOrNull()?.email
}

internal class LinkedInUserProvider(private val httpClient: HttpClient) : OAuthUserProvider {

    override suspend fun getUser(token: String): OAuthUser = coroutineScope {
        val emailJob = async {
            httpClient.get(EMAIL_ENDPOINT_URL) {
                parameter(key = "q", value = "members")
                parameter(
                    key = "projection",
                    value = "(elements*(primary,type,handle~))"
                )
                headers {
                    accept(ContentType.Application.Json)
                    bearerAuth(token)
                }
            }.body<EmailHandle>()
        }
        val userJob = async {
            httpClient.get(USER_ENDPOINT_URL) {
                parameter(
                    key = "projection",
                    value = "(id,localizedFirstName,localizedLastName,profilePicture(displayImage~:playableStreams))"
                )
                headers {
                    accept(ContentType.Application.Json)
                    bearerAuth(token)
                }
            }.body<LinkedInUser>()
        }
        userJob.await().apply {
            email = emailJob.await().bestEmail
        }
    }
}
