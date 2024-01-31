package io.newm.server.auth.oauth.repo

import io.newm.server.auth.oauth.model.OAuthTokens
import io.newm.server.auth.oauth.model.OAuthType

interface OAuthRepository {
    suspend fun getTokens(
        type: OAuthType,
        code: String,
        redirectUri: String?
    ): OAuthTokens
}
