package io.projectnewm.server.features.user.oauth

interface OAuthUserProvider {
    suspend fun getUser(token: String): OAuthUser
}
