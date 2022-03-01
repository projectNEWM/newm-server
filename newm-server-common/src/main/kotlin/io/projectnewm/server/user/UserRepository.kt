package io.projectnewm.server.user

import io.projectnewm.server.oauth.OAuthType

interface UserRepository {
    suspend fun registerUser(oauthType: OAuthType, accessToken: String): String
}
