package io.newm.server.auth.oauth.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OAuthTokens(
    @SerialName("access_token")
    val accessToken: String?,
    @SerialName("id_token")
    val idToken: String?,
)
