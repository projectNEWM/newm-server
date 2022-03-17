package io.projectnewm.server.auth.oauth

import kotlinx.serialization.Serializable

@Serializable
data class OAuthLoginResponse(val token: String)
