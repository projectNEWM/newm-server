package io.projectnewm.server.auth.oauth

interface OAuthRepository {
    suspend fun getAccessToken(type: OAuthType, code: String, redirectUri: String): String
}
