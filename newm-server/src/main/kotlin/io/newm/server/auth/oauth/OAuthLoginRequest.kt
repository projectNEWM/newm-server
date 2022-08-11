package io.newm.server.auth.oauth

import kotlinx.serialization.Serializable

@Serializable
data class OAuthLoginRequest(
    val accessToken: String? = null,
    val code: String? = null,
    val redirectUri: String? = null
)
