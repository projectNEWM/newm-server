package io.newm.server.auth.oauth.model

import kotlinx.serialization.Serializable

@Serializable
data class OAuthLoginRequest(
    val accessToken: String? = null,
    val idToken: String? = null,
    val code: String? = null,
    val redirectUri: String? = null
) {
    val oauthTokens: OAuthTokens?
        get() = if (accessToken != null || idToken != null) OAuthTokens(accessToken, idToken) else null
}
