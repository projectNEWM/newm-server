package io.projectnewm.server.auth.oauth.repo

import io.projectnewm.server.auth.oauth.OAuthType

interface OAuthRepository {
    suspend fun getAccessToken(type: OAuthType, code: String, redirectUri: String): String
}
