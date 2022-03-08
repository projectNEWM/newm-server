package io.projectnewm.server.user.oauth

interface OAuthUserProvider {
    suspend fun getUser(token: String): OAuthUser
}
