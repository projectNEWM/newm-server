package io.projectnewm.server.user

import io.projectnewm.server.oauth.OAuthType
import java.util.UUID

interface UserRepository {
    suspend fun findOrAdd(oauthType: OAuthType, accessToken: String): UUID
    suspend fun exists(userId: UUID): Boolean
    suspend fun get(userId: UUID, includeAll: Boolean = true): User
    suspend fun update(userId: UUID, user: User)
    suspend fun delete(userId: UUID)
}
