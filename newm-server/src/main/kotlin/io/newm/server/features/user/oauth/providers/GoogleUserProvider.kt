package io.newm.server.features.user.oauth.providers

import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
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
import io.newm.server.ktx.getSecureConfigStrings
import io.newm.server.ktx.toRSAKeyProvider
import io.newm.server.ktx.withAnyOfAudience
import io.newm.server.ktx.withAnyOfIssuer
import io.newm.shared.exception.HttpBadRequestException
import io.newm.shared.exception.HttpUnauthorizedException
import io.newm.shared.ktx.getConfigString
import io.newm.shared.ktx.getConfigStrings
import io.newm.shared.ktx.toUrl
import kotlinx.coroutines.runBlocking
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
    @SerialName("verified_email")
    override val isEmailVerified: Boolean? = null
) : OAuthUser

private class GoogleJwtUser(
    val jwt: DecodedJWT
) : OAuthUser {
    override val id: String
        get() = jwt.subject!!
    override val firstName: String?
        get() = jwt.getClaim("given_name").asString()
    override val lastName: String?
        get() = jwt.getClaim("family_name").asString()
    override val pictureUrl: String?
        get() = jwt.getClaim("picture").asString()
    override val email: String?
        get() = jwt.getClaim("email").asString()
    override val isEmailVerified: Boolean?
        get() = jwt.getClaim("email_verified").asBoolean()
}

internal class GoogleUserProvider(
    environment: ApplicationEnvironment,
    private val httpClient: HttpClient
) : OAuthUserProvider {
    private val verifier: JWTVerifier =
        JWT
            .require(
                Algorithm.RSA256(
                    JwkProviderBuilder(environment.getConfigString("oauth.google.publicKeysUrl").toUrl())
                        .build()
                        .toRSAKeyProvider()
                )
            ).withAnyOfIssuer(environment.getConfigStrings("oauth.google.issuers"))
            .withAnyOfAudience(runBlocking { environment.getSecureConfigStrings("oauth.google.audiences") })
            .withClaimPresence("sub")
            .withClaimPresence("email")
            .build()

    private val userInfoUrl = environment.getConfigString("oauth.google.userInfoUrl")

    override suspend fun getUser(tokens: OAuthTokens): OAuthUser =
        tokens.run {
            idToken?.let {
                try {
                    GoogleJwtUser(verifier.verify(idToken))
                } catch (exception: JWTVerificationException) {
                    throw HttpUnauthorizedException("Verification failed: ${exception.message}")
                }
            } ?: accessToken?.let {
                httpClient
                    .get(userInfoUrl) {
                        parameter(
                            key = "fields",
                            value = "id,given_name,family_name,picture,email,verified_email"
                        )
                        headers {
                            accept(ContentType.Application.Json)
                            bearerAuth(accessToken)
                        }
                    }.checkedBody<GoogleUser>()
            } ?: throw HttpBadRequestException("Google OAuth requires idToken or accessToken")
        }
}
