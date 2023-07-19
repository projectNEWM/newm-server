package io.newm.server.features.user.oauth.providers

import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import io.ktor.server.application.ApplicationEnvironment
import io.newm.server.auth.oauth.model.OAuthTokens
import io.newm.server.features.user.oauth.OAuthUser
import io.newm.server.features.user.oauth.OAuthUserProvider
import io.newm.server.ktx.getSecureConfigStrings
import io.newm.server.ktx.toRSAKeyProvider
import io.newm.server.ktx.withAnyOfAudience
import io.newm.shared.exception.HttpBadRequestException
import io.newm.shared.exception.HttpUnauthorizedException
import io.newm.shared.ktx.getConfigString
import kotlinx.coroutines.runBlocking

private data class AppleUser(
    val jwt: DecodedJWT
) : OAuthUser {

    override val id: String
        get() = jwt.subject!!

    override val firstName: String? = null
    override val lastName: String? = null
    override val pictureUrl: String? = null
    override val email: String?
        get() = jwt.getClaim("email").asString()
    override val isEmailVerified: Boolean?
        get() = jwt.getClaim("email_verified")?.let { it.asBoolean() ?: it.asString()?.toBoolean() }
}

internal class AppleUserProvider(
    environment: ApplicationEnvironment
) : OAuthUserProvider {

    private val verifier: JWTVerifier = JWT.require(
        Algorithm.RSA256(
            JwkProviderBuilder(environment.getConfigString("oauth.apple.publicKeysUrl"))
                .build()
                .toRSAKeyProvider()
        )
    ).withIssuer("https://appleid.apple.com")
        .withAnyOfAudience(runBlocking { environment.getSecureConfigStrings("oauth.apple.audiences") })
        .withClaimPresence("sub")
        .withClaimPresence("email")
        .build()

    override suspend fun getUser(tokens: OAuthTokens): OAuthUser {
        val token = tokens.idToken ?: throw HttpBadRequestException("Apple OAuth requires idToken")
        try {
            return AppleUser(verifier.verify(token))
        } catch (exception: JWTVerificationException) {
            throw HttpUnauthorizedException("Verification failed: ${exception.message}")
        }
    }
}
