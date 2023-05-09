package io.newm.server.features.user.oauth

import io.newm.server.auth.oauth.model.OAuthTokens

interface OAuthUserProvider {
    suspend fun getUser(tokens: OAuthTokens): OAuthUser
}
