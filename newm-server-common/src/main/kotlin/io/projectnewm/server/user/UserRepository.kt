package io.projectnewm.server.user

import io.projectnewm.server.auth.oauth.OAuthType
import java.util.UUID

interface UserRepository {
    suspend fun add(user: User)
    suspend fun find(email: String, password: Password): UUID
    suspend fun findOrAdd(oauthType: OAuthType, accessToken: String): UUID
    suspend fun exists(userId: UUID): Boolean
    suspend fun get(userId: UUID, includeAll: Boolean = true): User
    suspend fun update(userId: UUID, user: User)
    suspend fun recover(user: User)
    suspend fun delete(userId: UUID)
}
