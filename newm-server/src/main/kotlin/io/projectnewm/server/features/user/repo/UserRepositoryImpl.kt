package io.projectnewm.server.features.user.repo

import io.ktor.util.logging.Logger
import io.projectnewm.server.auth.oauth.OAuthType
import io.projectnewm.server.auth.twofactor.repo.TwoFactorAuthRepository
import io.projectnewm.server.exception.HttpBadRequestException
import io.projectnewm.server.exception.HttpConflictException
import io.projectnewm.server.exception.HttpForbiddenException
import io.projectnewm.server.exception.HttpNotFoundException
import io.projectnewm.server.exception.HttpUnauthorizedException
import io.projectnewm.server.exception.HttpUnprocessableEntityException
import io.projectnewm.server.ext.exists
import io.projectnewm.server.ext.isValidEmail
import io.projectnewm.server.ext.isValidUrl
import io.projectnewm.server.features.user.database.UserEntity
import io.projectnewm.server.features.user.model.Password
import io.projectnewm.server.features.user.model.User
import io.projectnewm.server.features.user.oauth.providers.FacebookUserProvider
import io.projectnewm.server.features.user.oauth.providers.GoogleUserProvider
import io.projectnewm.server.features.user.oauth.providers.LinkedInUserProvider
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

    override suspend fun add(user: User) {
        logger.debug(marker, "add: user = $user")

        val firstName = user.firstName.asValidName()
        val lastName = user.lastName.asValidName()
        val pictureUrl = user.pictureUrl?.asValidUrl()
        val email = user.email.asValidEmail().asVerifiedEmail(user.authCode)
        val passwordHash = user.newPassword.asValidPassword(user.confirmPassword).toHash()

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

    override suspend fun find(email: String, password: Password): UUID = transaction {
        logger.debug(marker, "find: email = $email")
        val entity = getUserEntityByEmail(email)
        password.checkAuth(entity.passwordHash)
        entity.id.value
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

    override suspend fun update(userId: UUID, user: User) {
        logger.debug(marker, "update: userId = $userId, user = $user")

        val firstName = user.firstName?.asValidName()
        val lastName = user.lastName?.asValidName()
        val pictureUrl = user.pictureUrl?.asValidUrl()
        val email = user.email?.asValidEmail()?.asVerifiedEmail(user.authCode)
        val passwordHash = user.newPassword?.asValidPassword(user.confirmPassword)?.toHash()

        transaction {
            val entity = UserEntity[userId]
            firstName?.let { entity.firstName = it }
            lastName?.let { entity.lastName = it }
            pictureUrl?.let { entity.pictureUrl = it }
            email?.let {
                it.checkEmailUnique()
                entity.email = it
            }
            passwordHash?.let {
                user.currentPassword.checkAuth(entity.passwordHash)
                entity.passwordHash = it
            }
        }
    }

    override suspend fun recover(user: User) {
        logger.debug(marker, "recover: user = $user")

        val email = user.email.asValidEmail().asVerifiedEmail(user.authCode)
        val passwordHash = user.newPassword.asValidPassword(user.confirmPassword).toHash()

        transaction {
            val entity = getUserEntityByEmail(email)
            entity.checkNonOAuth()
            entity.passwordHash = passwordHash
        }
    }

    override suspend fun delete(userId: UUID) = transaction {
        logger.debug(marker, "delete: userId = $userId")
        UserEntity[userId].delete()
    }

    private fun getUserEntityByEmail(email: String): UserEntity =
        UserEntity.getByEmail(email) ?: throw HttpNotFoundException("Doesn't exist: $email")

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

    private fun Password?.asValidPassword(confirm: Password?): Password {
        if (this == null || value.isBlank()) throw HttpBadRequestException("Missing password")
        if (confirm?.value.isNullOrBlank()) throw HttpBadRequestException("Missing password confirmation")
        if (this != confirm) throw HttpUnprocessableEntityException("Password confirmation failed")
        if (value.length < 8) throw HttpUnprocessableEntityException("Password too short")
        return this
    }

    private suspend fun String.asVerifiedEmail(code: String?): String {
        if (code == null) throw HttpBadRequestException("Missing 2FA code")
        if (!twoFactorAuthRepository.verifyCode(this, code)) throw HttpForbiddenException("2FA failed")
        return this
    }

    private fun String.checkEmailUnique() {
        if (UserEntity.existsByEmail(this)) throw HttpConflictException("Already exists: $this")
    }

    private fun Password?.checkAuth(hash: String?) {
        if (this == null) throw HttpBadRequestException("Missing password")
        if (hash == null || !verify(hash)) throw HttpUnauthorizedException("Invalid password")
    }

    private fun UserEntity.checkNonOAuth() {
        if (oauthType != null) throw HttpForbiddenException("Not allowed for OAuth Users")
    }
}
