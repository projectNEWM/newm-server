package io.projectnewm.server.user.repo

import io.ktor.util.logging.Logger
import io.projectnewm.server.auth.oauth.OAuthType
import io.projectnewm.server.auth.twofactor.TwoFactorAuthRepository
import io.projectnewm.server.exception.HttpBadRequestException
import io.projectnewm.server.exception.HttpConflictException
import io.projectnewm.server.exception.HttpUnauthorizedException
import io.projectnewm.server.exception.HttpUnprocessableEntityException
import io.projectnewm.server.ext.exists
import io.projectnewm.server.ext.isValidEmail
import io.projectnewm.server.ext.isValidUrl
import io.projectnewm.server.ext.toHash
import io.projectnewm.server.ext.verify
import io.projectnewm.server.user.User
import io.projectnewm.server.user.UserRepository
import io.projectnewm.server.user.database.UserEntity
import io.projectnewm.server.user.oauth.providers.FacebookUserProvider
import io.projectnewm.server.user.oauth.providers.GoogleUserProvider
import io.projectnewm.server.user.oauth.providers.LinkedInUserProvider
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.MarkerFactory
import java.util.UUID

internal class UserRepositoryImpl(
    private val logger: Logger,
    private val googleUserProvider: GoogleUserProvider,
    private val facebookUserProvider: FacebookUserProvider,
    private val linkedInUserProvider: LinkedInUserProvider,
    private val twoFactorAuthRepository: TwoFactorAuthRepository
) : UserRepository {

    private val marker = MarkerFactory.getMarker(javaClass.simpleName)

    override suspend fun add(user: User, authCode: String) {
        logger.debug(marker, "add: user = $user")

        val firstName = user.firstName.asValidName()
        val lastName = user.lastName.asValidName()
        val pictureUrl = user.pictureUrl?.asValidUrl()
        val email = user.email.asValidEmail().asVerifiedEmail(authCode)
        val passwordHash = user.password.asValidPassword().toHash()

        transaction {
            email.checkEmailUnique()

            UserEntity.new {
                this.firstName = firstName
                this.lastName = lastName
                this.pictureUrl = pictureUrl
                this.email = email
                this.passwordHash = passwordHash
            }
        }
    }

    override suspend fun find(email: String, password: String): UUID? = transaction {
        logger.debug(marker, "find: email = $email")
        UserEntity.getByEmail(email)?.takeIf {
            val hash = it.passwordHash
            hash != null && password.verify(hash)
        }?.id?.value
    }

    override suspend fun findOrAdd(oauthType: OAuthType, accessToken: String): UUID {
        logger.debug(marker, "findOrAdd: oauthType = $oauthType")

        val user = when (oauthType) {
            OAuthType.Google -> googleUserProvider.getUser(accessToken)
            OAuthType.Facebook -> facebookUserProvider.getUser(accessToken)
            OAuthType.LinkedIn -> linkedInUserProvider.getUser(accessToken)
        }
        logger.debug(marker, "findOrAdd: oauthUser = $user")

        val firstName = user.firstName.asValidName()
        val lastName = user.lastName.asValidName()
        val pictureUrl = user.pictureUrl?.asValidUrl()
        val email = user.email.asValidEmail()

        return transaction {
            UserEntity.getId(
                oauthType = oauthType,
                oauthId = user.id
            ) ?: let {
                email.checkEmailUnique()
                UserEntity.new {
                    this.oauthType = oauthType
                    this.oauthId = user.id
                    this.firstName = firstName
                    this.lastName = lastName
                    this.pictureUrl = pictureUrl
                    this.email = email
                }.id.value
            }
        }
    }

    override suspend fun exists(userId: UUID): Boolean = transaction {
        UserEntity.exists(userId)
    }

    override suspend fun get(userId: UUID, includeAll: Boolean): User = transaction {
        logger.debug(marker, "get: userId = $userId, includeAll = $includeAll")
        UserEntity[userId].toModel(includeAll)
    }

    override suspend fun update(userId: UUID, user: User, authCode: String?) {
        logger.debug(marker, "update: userId = $userId, user = $user")

        val firstName = user.firstName?.asValidName()
        val lastName = user.lastName?.asValidName()
        val pictureUrl = user.pictureUrl?.asValidUrl()
        val email = user.email?.asValidEmail()?.asVerifiedEmail(authCode)
        val passwordHash = user.password?.asValidPassword()?.toHash()

        transaction {
            email?.checkEmailUnique()

            val entity = UserEntity[userId]
            firstName?.let { entity.firstName = it }
            lastName?.let { entity.lastName = it }
            pictureUrl?.let { entity.pictureUrl = it }
            email?.let { entity.email = it }
            passwordHash?.let { entity.passwordHash = it }
        }
    }

    override suspend fun delete(userId: UUID) = transaction {
        logger.debug(marker, "delete: userId = $userId")
        UserEntity[userId].delete()
    }

    private fun String?.asValidName(): String {
        if (isNullOrBlank()) throw HttpBadRequestException("Missing name")
        return this
    }

    private fun String?.asValidEmail(): String {
        if (isNullOrBlank()) throw HttpBadRequestException("Missing email")
        if (!isValidEmail()) throw HttpUnprocessableEntityException("Invalid email: $this")
        return this
    }

    private fun String?.asValidUrl(): String {
        if (isNullOrBlank()) throw HttpBadRequestException("Missing url")
        if (!isValidUrl()) throw HttpUnprocessableEntityException("Invalid url: $this")
        return this
    }

    private fun String?.asValidPassword(): String {
        if (isNullOrBlank()) throw HttpBadRequestException("Missing password")
        if (length < 8) throw HttpUnprocessableEntityException("Password too short")
        return this
    }

    private suspend fun String.asVerifiedEmail(code: String?): String {
        if (code == null) throw HttpBadRequestException("Missing 2FA code")
        if (!twoFactorAuthRepository.verifyCode(this, code)) throw HttpUnauthorizedException("2FA failed")
        return this
    }

    private fun String.checkEmailUnique() {
        if (UserEntity.existsByEmail(this)) throw HttpConflictException("Already exists: $this")
    }
}
