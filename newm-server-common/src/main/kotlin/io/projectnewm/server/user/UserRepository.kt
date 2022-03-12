package io.projectnewm.server.user

import io.projectnewm.server.auth.oauth.OAuthType
import java.util.UUID

interface UserRepository {
    suspend fun add(user: User, authCode: String)
    suspend fun find(email: String, password: String): UUID?
    suspend fun findOrAdd(oauthType: OAuthType, accessToken: String): UUID
    suspend fun exists(userId: UUID): Boolean
    suspend fun get(userId: UUID, includeAll: Boolean = true): User
    suspend fun update(userId: UUID, user: User, authCode: String? = null)
    suspend fun delete(userId: UUID)
}
