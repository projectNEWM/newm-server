package io.newm.server.features.user.repo

import io.newm.server.auth.oauth.OAuthType
import io.newm.server.auth.password.Password
import io.newm.server.features.user.model.User
import java.util.UUID

interface UserRepository {
    suspend fun add(user: User)
    suspend fun find(email: String, password: Password): UUID
    suspend fun findOrAdd(oauthType: OAuthType, oauthAccessToken: String): UUID
    suspend fun exists(userId: UUID): Boolean
    suspend fun get(userId: UUID, includeAll: Boolean = true): User
    suspend fun update(userId: UUID, user: User)
    suspend fun recover(user: User)
    suspend fun delete(userId: UUID)
}
