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

    override val pictureUrl: String?
        get() = picture?.image?.elements?.run {
            firstOrNull { it.isPreferred } ?: firstOrNull()
        }?.identifiers?.firstOrNull()?.identifier

    override var email: String? = null

    override var isEmailVerified: Boolean? = null

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
                @SerialName("data")
                val data: Data?,
                @SerialName("identifiers")
                val identifiers: List<Identifier>?
            ) {
                @Serializable
                data class Data(
                    @SerialName("com.linkedin.digitalmedia.mediaartifact.StillImage")
                    val stillImage: StillImage?
                )

                @Serializable
                data class StillImage(
                    @SerialName("displaySize")
                    val displaySize: DisplaySize?
                )

                @Serializable
                data class DisplaySize(
                    @SerialName("width")
                    val with: Double?,
                    @SerialName("height")
                    val height: Double?
                )

                @Serializable
                data class Identifier(
                    @SerialName("identifier")
                    val identifier: String?
                )

                val isPreferred: Boolean
                    get() = data?.stillImage?.displaySize?.run { with == 200.0 && height == 200.0 } == true
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
        val primary: Boolean?,
        @SerialName("type")
        val type: String?
    ) {
        @Serializable
        data class Handle(
            @SerialName("emailAddress")
            val emailAddress: String?,
        )
    }

    val bestElement: Element?
        get() = elements?.firstOrNull { it.primary == true } ?: elements?.firstOrNull()
}

internal class LinkedInUserProvider(
    environment: ApplicationEnvironment,
    private val httpClient: HttpClient
) : OAuthUserProvider {

    private val userInfoUrl = environment.getConfigString("oauth.linkedin.userInfoUrl")
    private val userExtraInfoUrl = environment.getConfigString("oauth.linkedin.userExtraInfoUrl")

    override suspend fun getUser(tokens: OAuthTokens): OAuthUser = coroutineScope {
        val token = tokens.accessToken ?: throw HttpBadRequestException("LinkedIn OAuth requires accessToken")
        val emailJob = async {
            httpClient.get(userExtraInfoUrl) {
                parameter(key = "q", value = "members")
                parameter(
                    key = "projection",
                    value = "(elements*(primary,type,handle~))"
                )
                headers {
                    accept(ContentType.Application.Json)
                    bearerAuth(token)
                }
            }.checkedBody<EmailHandle>()
        }
        val userJob = async {
            httpClient.get(userInfoUrl) {
                parameter(
                    key = "projection",
                    value = "(id,localizedFirstName,localizedLastName,profilePicture(displayImage~:playableStreams))"
                )
                headers {
                    accept(ContentType.Application.Json)
                    bearerAuth(token)
                }
            }.checkedBody<LinkedInUser>()
        }
        val emailElement = emailJob.await().bestElement
        userJob.await().apply {
            email = emailElement?.handle?.emailAddress
            isEmailVerified = emailElement?.type == "EMAIL" // "OTHER" if unverified
        }
    }
}
