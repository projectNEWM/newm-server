package io.projectnewm.server.user.repo

import io.ktor.util.logging.Logger
import io.projectnewm.server.ext.exists
import io.projectnewm.server.oauth.OAuthType
import io.projectnewm.server.user.User
import io.projectnewm.server.user.UserRepository
import io.projectnewm.server.user.database.UserEntity
import io.projectnewm.server.user.database.find
import io.projectnewm.server.user.oauth.providers.FacebookUserProvider
import io.projectnewm.server.user.oauth.providers.GoogleUserProvider
import io.projectnewm.server.user.oauth.providers.LinkedInUserProvider
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

internal class UserRepositoryImpl(
    private val logger: Logger,
    private val googleUserProvider: GoogleUserProvider,
    private val facebookUserProvider: FacebookUserProvider,
    private val linkedInUserProvider: LinkedInUserProvider
) : UserRepository {

    override suspend fun findOrAdd(oauthType: OAuthType, accessToken: String): UUID {
        logger.debug("findOrAdd: oauthType = $oauthType, accessToken = $accessToken")

        val oauthUser = when (oauthType) {
            OAuthType.Google -> googleUserProvider.getUser(accessToken)
            OAuthType.Facebook -> facebookUserProvider.getUser(accessToken)
            OAuthType.LinkedIn -> linkedInUserProvider.getUser(accessToken)
        }
        logger.debug("findOrAdd: oauthUser = $oauthUser")

        return transaction {
            val user = UserEntity.find(
                oauthType = oauthType,
                oauthId = oauthUser.id
            ) ?: UserEntity.new {
                this.oauthType = oauthType
                oauthId = oauthUser.id
                firstName = oauthUser.firstName
                lastName = oauthUser.lastName
                pictureUrl = oauthUser.pictureUrl
                email = oauthUser.email
            }
            user.id.value
        }
    }

    override suspend fun exists(userId: UUID): Boolean = transaction {
        UserEntity.exists(userId)
    }

    override suspend fun get(userId: UUID, includeAll: Boolean): User = transaction {
        logger.debug("get: userId = $userId, includeAll = $includeAll")
        UserEntity[userId].toModel(includeAll)
    }

    override suspend fun update(userId: UUID, user: User): Unit = transaction {
        logger.debug("update: userId = $userId, user = $user")

        val entity = UserEntity[userId]
        with(user) {
            firstName?.let { entity.firstName = it }
            lastName?.let { entity.lastName = it }
            pictureUrl?.let { entity.pictureUrl = it }
            email?.let { entity.email = it }
        }
    }

    override suspend fun delete(userId: UUID) = transaction {
        logger.debug("delete: userId = $userId")
        UserEntity[userId].delete()
    }
}
