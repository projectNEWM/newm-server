package io.newm.server.features.user.repo

import io.newm.server.auth.oauth.OAuthType
import io.newm.shared.auth.Password
import io.newm.server.features.user.model.User
import io.newm.server.features.user.model.UserFilters
import java.util.UUID

interface UserRepository {
    suspend fun add(user: User)
    suspend fun find(email: String, password: Password): UUID
    suspend fun findOrAdd(oauthType: OAuthType, oauthAccessToken: String): UUID
    suspend fun exists(userId: UUID): Boolean
    suspend fun get(userId: UUID, includeAll: Boolean = true): User
    suspend fun getAll(filters: UserFilters, offset: Int, limit: Int): List<User>
    suspend fun update(userId: UUID, user: User)
    suspend fun recover(user: User)
    suspend fun delete(userId: UUID)
}
