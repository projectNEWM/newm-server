package io.newm.server.features.user.repo

import io.ktor.util.logging.Logger
import io.newm.server.auth.oauth.OAuthType
import io.newm.server.auth.twofactor.repo.TwoFactorAuthRepository
import io.newm.server.features.user.database.UserEntity
import io.newm.server.features.user.model.User
import io.newm.server.features.user.model.UserFilters
import io.newm.server.features.user.oauth.providers.FacebookUserProvider
import io.newm.server.features.user.oauth.providers.GoogleUserProvider
import io.newm.server.features.user.oauth.providers.LinkedInUserProvider
import io.newm.server.ktx.asValidEmail
import io.newm.server.ktx.asValidUrl
import io.newm.server.ktx.checkLength
import io.newm.shared.auth.Password
import io.newm.shared.exception.HttpBadRequestException
import io.newm.shared.exception.HttpConflictException
import io.newm.shared.exception.HttpForbiddenException
import io.newm.shared.exception.HttpNotFoundException
import io.newm.shared.exception.HttpUnauthorizedException
import io.newm.shared.exception.HttpUnprocessableEntityException
import io.newm.shared.koin.inject
import io.newm.shared.ktx.debug
import io.newm.shared.ktx.existsHavingId
import io.newm.shared.ktx.isValidPassword
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.parameter.parametersOf
import java.util.UUID

internal class UserRepositoryImpl(
    private val googleUserProvider: GoogleUserProvider,
    private val facebookUserProvider: FacebookUserProvider,
    private val linkedInUserProvider: LinkedInUserProvider,
    private val twoFactorAuthRepository: TwoFactorAuthRepository
) : UserRepository {

    private val logger: Logger by inject { parametersOf(javaClass.simpleName) }

    override suspend fun add(user: User) {
        logger.debug { "add: user = $user" }

        user.checkFieldLengths()
        val email = user.email.asValidEmail().asVerifiedEmail(user.authCode)
        val passwordHash = user.newPassword.asValidPassword(user.confirmPassword).toHash()

        transaction {
            email.checkEmailUnique()
            UserEntity.new {
                this.firstName = user.firstName
                this.lastName = user.lastName
                this.nickname = user.nickname
                this.pictureUrl = user.pictureUrl?.asValidUrl()
                this.bannerUrl = user.bannerUrl?.asValidUrl()
                this.websiteUrl = user.websiteUrl?.asValidUrl()
                this.twitterUrl = user.twitterUrl?.asValidUrl()
                this.instagramUrl = user.instagramUrl?.asValidUrl()
                this.location = user.location
                this.role = user.role
                this.genre = user.genre
                this.biography = user.biography
                this.walletAddress = user.walletAddress
                this.email = email
                this.passwordHash = passwordHash
                this.companyName = user.companyName
                this.companyLogoUrl = user.companyLogoUrl.asValidUrl()
                this.companyIpRights = user.companyIpRights
            }
        }
    }

    override suspend fun find(email: String, password: Password): UUID {
        logger.debug { "find: email = $email" }
        return transaction {
            val entity = getUserEntityByEmail(email)
            entity.checkNonOAuth()
            password.checkAuth(entity.passwordHash)
            entity.id.value
        }
    }

    override suspend fun findOrAdd(oauthType: OAuthType, oauthAccessToken: String): UUID {
        logger.debug { "findOrAdd: oauthType = $oauthType" }

        val user = when (oauthType) {
            OAuthType.Google -> googleUserProvider.getUser(oauthAccessToken)
            OAuthType.Facebook -> facebookUserProvider.getUser(oauthAccessToken)
            OAuthType.LinkedIn -> linkedInUserProvider.getUser(oauthAccessToken)
        }
        logger.debug { "findOrAdd: oauthUser = $user" }

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
                    this.firstName = user.firstName
                    this.lastName = user.lastName
                    this.pictureUrl = pictureUrl
                    this.email = email
                }.id.value
            }
        }
    }

    override suspend fun exists(userId: UUID): Boolean = transaction {
        UserEntity.existsHavingId(userId)
    }

    override suspend fun get(userId: UUID, includeAll: Boolean): User {
        logger.debug { "get: userId = $userId, includeAll = $includeAll" }
        return transaction {
            UserEntity[userId].toModel(includeAll)
        }
    }

    override suspend fun getAll(filters: UserFilters, offset: Int, limit: Int): List<User> {
        logger.debug { "getAll: filters = $filters, offset = $offset, limit = $limit" }
        return transaction {
            UserEntity.all(filters)
                .limit(n = limit, offset = offset.toLong())
                .map { it.toModel(includeAll = false) }
        }
    }

    override suspend fun getAllCount(filters: UserFilters): Long {
        logger.debug { "getAllCount: filters = $filters" }
        return transaction {
            UserEntity.all(filters).count()
        }
    }

    override suspend fun update(userId: UUID, user: User) {
        logger.debug { "update: userId = $userId, user = $user" }

        user.checkFieldLengths()
        val email = user.email?.asValidEmail()?.asVerifiedEmail(user.authCode)
        val passwordHash = user.newPassword?.asValidPassword(user.confirmPassword)?.toHash()

        transaction {
            val entity = UserEntity[userId]
            user.firstName?.let { entity.firstName = it }
            user.lastName?.let { entity.lastName = it }
            user.nickname?.let { entity.nickname = it }
            user.pictureUrl?.let { entity.pictureUrl = it.asValidUrl() }
            user.bannerUrl?.let { entity.bannerUrl = it.asValidUrl() }
            user.websiteUrl?.let { entity.websiteUrl = it.asValidUrl() }
            user.twitterUrl?.let { entity.twitterUrl = it.asValidUrl() }
            user.instagramUrl?.let { entity.instagramUrl = it.asValidUrl() }
            user.location?.let { entity.location = it }
            user.role?.let { entity.role = it }
            user.genre?.let { entity.genre = it }
            user.biography?.let { entity.biography = it }
            user.walletAddress?.let { entity.walletAddress = it }
            email?.let {
                it.checkEmailUnique()
                entity.email = it
            }
            passwordHash?.let {
                entity.checkNonOAuth()
                user.currentPassword.checkAuth(entity.passwordHash)
                entity.passwordHash = it
            }
            user.companyName?.let { entity.companyName = it }
            user.companyLogoUrl?.let { entity.companyLogoUrl = it.asValidUrl() }
            user.companyIpRights?.let { entity.companyIpRights = it }
        }
    }

    override suspend fun recover(user: User) {
        logger.debug { "recover: user = $user" }

        val email = user.email.asValidEmail().asVerifiedEmail(user.authCode)
        val passwordHash = user.newPassword.asValidPassword(user.confirmPassword).toHash()

        transaction {
            val entity = getUserEntityByEmail(email)
            entity.checkNonOAuth()
            entity.passwordHash = passwordHash
        }
    }

    override suspend fun delete(userId: UUID) {
        logger.debug { "delete: userId = $userId" }
        transaction {
            UserEntity[userId].delete()
        }
    }

    private fun getUserEntityByEmail(email: String): UserEntity =
        UserEntity.getByEmail(email) ?: throw HttpNotFoundException("Doesn't exist: $email")

    private fun Password?.asValidPassword(confirm: Password?): Password {
        if (this == null || value.isBlank()) throw HttpBadRequestException("Missing password")
        if (confirm?.value.isNullOrBlank()) throw HttpBadRequestException("Missing password confirmation")
        if (this != confirm) throw HttpUnprocessableEntityException("Password confirmation failed")
        if (!this.value.isValidPassword()) throw HttpUnprocessableEntityException("Password must contain at least 8 characters, 1 uppercase letter, 1 lowercase letter and 1 number.")
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

    private fun User.checkFieldLengths() {
        firstName?.checkLength("firstName")
        lastName?.checkLength("lastName")
        nickname?.checkLength("nickname")
        location?.checkLength("location")
        role?.checkLength("role")
        genre?.checkLength("genre")
        biography?.checkLength("biography", 250)
        companyName?.checkLength("companyName")
    }
}
