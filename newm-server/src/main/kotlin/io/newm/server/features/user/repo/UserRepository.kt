package io.newm.server.features.user.repo

import io.newm.server.auth.oauth.model.OAuthTokens
import io.newm.server.auth.oauth.model.OAuthType
import io.newm.server.features.user.model.User
import io.newm.server.features.user.model.UserFilters
import io.newm.server.model.ClientPlatform
import io.newm.server.typealiases.UserId
import io.newm.shared.auth.Password

interface UserRepository {
    suspend fun add(
        user: User,
        clientPlatform: ClientPlatform?
    ): UserId

    suspend fun find(
        email: String,
        password: Password
    ): Pair<UserId, Boolean>

    suspend fun findByEmail(email: String): User

    suspend fun findOrAdd(
        oauthType: OAuthType,
        oauthTokens: OAuthTokens,
        clientPlatform: ClientPlatform?
    ): UserId

    suspend fun exists(userId: UserId): Boolean

    suspend fun get(
        userId: UserId,
        includeAll: Boolean = true
    ): User

    suspend fun getAll(
        filters: UserFilters,
        offset: Int,
        limit: Int
    ): List<User>

    suspend fun getAllCount(filters: UserFilters): Long

    suspend fun isAdmin(userId: UserId): Boolean

    suspend fun update(
        userId: UserId,
        user: User
    )

    fun updateUserData(
        userId: UserId,
        user: User
    )

    suspend fun recover(user: User)

    suspend fun delete(userId: UserId)
}
