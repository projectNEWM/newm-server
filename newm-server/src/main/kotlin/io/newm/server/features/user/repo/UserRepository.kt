package io.newm.server.features.user.repo

import io.newm.server.auth.oauth.model.OAuthTokens
import io.newm.server.auth.oauth.model.OAuthType
import io.newm.shared.auth.Password
import io.newm.server.features.user.model.User
import io.newm.server.features.user.model.UserFilters
import java.util.UUID

interface UserRepository {
    suspend fun add(user: User): UUID

    suspend fun find(
        email: String,
        password: Password
    ): Pair<UUID, Boolean>

    suspend fun findByEmail(email: String): User

    suspend fun findOrAdd(
        oauthType: OAuthType,
        oauthTokens: OAuthTokens
    ): UUID

    suspend fun exists(userId: UUID): Boolean

    suspend fun get(
        userId: UUID,
        includeAll: Boolean = true
    ): User

    suspend fun getAll(
        filters: UserFilters,
        offset: Int,
        limit: Int
    ): List<User>

    suspend fun getAllCount(filters: UserFilters): Long

    suspend fun update(
        userId: UUID,
        user: User
    )

    fun updateUserData(
        userId: UUID,
        user: User
    )

    suspend fun recover(user: User)

    suspend fun delete(userId: UUID)
}
