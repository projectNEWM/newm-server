package io.newm.server.auth.oauth.repo

import io.newm.server.auth.oauth.OAuthType

interface OAuthRepository {
    suspend fun getAccessToken(type: OAuthType, code: String, redirectUri: String): String
}
