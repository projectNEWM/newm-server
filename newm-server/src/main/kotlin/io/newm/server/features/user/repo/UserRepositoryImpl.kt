package io.newm.server.features.user.repo

import io.ktor.util.logging.Logger
import io.newm.server.auth.oauth.model.OAuthTokens
import io.newm.server.auth.oauth.model.OAuthType
import io.newm.server.auth.twofactor.repo.TwoFactorAuthRepository
import io.newm.server.config.repo.ConfigRepository
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_EMAIL_WHITELIST
import io.newm.server.features.user.database.UserEntity
import io.newm.server.features.user.model.User
import io.newm.server.features.user.model.UserFilters
import io.newm.server.features.user.oauth.OAuthUser
import io.newm.server.features.user.oauth.providers.AppleUserProvider
import io.newm.server.features.user.oauth.providers.FacebookUserProvider
import io.newm.server.features.user.oauth.providers.GoogleUserProvider
import io.newm.server.features.user.oauth.providers.LinkedInUserProvider
import io.newm.server.features.user.verify.OutletProfileUrlVerifier
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
import io.newm.shared.ktx.orNull
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.parameter.parametersOf
import java.util.UUID

internal class UserRepositoryImpl(
    private val googleUserProvider: GoogleUserProvider,
    private val facebookUserProvider: FacebookUserProvider,
    private val linkedInUserProvider: LinkedInUserProvider,
    private val appleUserProvider: AppleUserProvider,
    private val twoFactorAuthRepository: TwoFactorAuthRepository,
    private val configRepository: ConfigRepository,
    private val spotifyProfileUrlVerifier: OutletProfileUrlVerifier,
    private val appleMusicProfileUrlVerifier: OutletProfileUrlVerifier,
    private val soundCloudProfileUrlVerifier: OutletProfileUrlVerifier,
) : UserRepository {

    private val logger: Logger by inject { parametersOf(javaClass.simpleName) }

    override suspend fun add(user: User): UUID {
        logger.debug { "add: user = $user" }

        user.checkWhitelist()
        user.checkFieldLengths()
        val email = user.email.asValidEmail().asVerifiedEmail(user.authCode)
        val passwordHash = user.newPassword.asValidPassword(user.confirmPassword).toHash()
        user.spotifyProfile?.let {
            spotifyProfileUrlVerifier.verify(it, user.stageOrFullName)
        }
        user.appleMusicProfile?.let {
            appleMusicProfileUrlVerifier.verify(it, user.stageOrFullName)
        }
        user.soundCloudProfile?.let {
            soundCloudProfileUrlVerifier.verify(it, user.stageOrFullName)
        }

        return transaction {
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
                this.spotifyProfile = user.spotifyProfile
                this.soundCloudProfile = user.soundCloudProfile
                this.appleMusicProfile = user.appleMusicProfile
                this.location = user.location
                this.role = user.role
                this.genre = user.genre
                this.biography = user.biography
                this.walletAddress = user.walletAddress
                this.email = email
                this.passwordHash = passwordHash
                this.companyName = user.companyName
                this.companyLogoUrl = user.companyLogoUrl?.asValidUrl()
                this.companyIpRights = user.companyIpRights
            }.id.value
        }
    }

    override suspend fun find(email: String, password: Password): Pair<UUID, Boolean> {
        logger.debug { "find: email = $email" }
        return transaction {
            val entity = getUserEntityByEmail(email)
            password.checkAuth(entity.passwordHash)
            entity.oauthType = null
            entity.oauthId = null
            entity.id.value to entity.admin
        }
    }

    override suspend fun findByEmail(email: String): User {
        logger.debug { "findByEmail: email = $email" }
        return transaction {
            getUserEntityByEmail(email).toModel()
        }
    }

    override suspend fun findOrAdd(oauthType: OAuthType, oauthTokens: OAuthTokens): UUID {
        logger.debug { "findOrAdd: oauthType = $oauthType" }

        val user = when (oauthType) {
            OAuthType.Google -> googleUserProvider.getUser(oauthTokens)
            OAuthType.Facebook -> facebookUserProvider.getUser(oauthTokens)
            OAuthType.LinkedIn -> linkedInUserProvider.getUser(oauthTokens)
            OAuthType.Apple -> appleUserProvider.getUser(oauthTokens)
        }
        logger.debug { "findOrAdd: oauthUser = $user" }

        user.checkWhitelist()
        val email = user.email.asValidEmail()
        if (user.isEmailVerified != true) {
            throw HttpUnauthorizedException("Unverified email: $email")
        }
        return transaction {
            val entity = UserEntity.getByEmail(email) ?: UserEntity.new {
                this.firstName = user.firstName
                this.lastName = user.lastName
                this.pictureUrl = user.pictureUrl?.asValidUrl()
                this.email = email
            }
            entity.oauthType = oauthType
            entity.oauthId = user.id
            entity.id.value
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

        newSuspendedTransaction {
            val entity = UserEntity[userId]
            user.firstName?.let { entity.firstName = it.orNull() }
            user.lastName?.let { entity.lastName = it.orNull() }
            user.nickname?.let { entity.nickname = it.orNull() }
            user.pictureUrl?.let { entity.pictureUrl = it.orNull()?.asValidUrl() }
            user.bannerUrl?.let { entity.bannerUrl = it.orNull()?.asValidUrl() }
            user.websiteUrl?.let { entity.websiteUrl = it.orNull()?.asValidUrl() }
            user.twitterUrl?.let { entity.twitterUrl = it.orNull()?.asValidUrl() }
            user.instagramUrl?.let { entity.instagramUrl = it.orNull()?.asValidUrl() }
            user.spotifyProfile?.let {
                entity.spotifyProfile = it.orNull()?.also { profile ->
                    spotifyProfileUrlVerifier.verify(profile, entity.stageOrFullName)
                }
            }
            user.soundCloudProfile?.let {
                entity.soundCloudProfile = it.orNull()?.also { profile ->
                    soundCloudProfileUrlVerifier.verify(profile, entity.stageOrFullName)
                }
            }
            user.appleMusicProfile?.let {
                entity.appleMusicProfile = it.orNull()?.also { profile ->
                    appleMusicProfileUrlVerifier.verify(profile, entity.stageOrFullName)
                }
            }
            user.location?.let { entity.location = it.orNull() }
            user.role?.let { entity.role = it.orNull() }
            user.genre?.let { entity.genre = it.orNull() }
            user.biography?.let { entity.biography = it.orNull() }
            user.walletAddress?.let { entity.walletAddress = it.orNull() }
            email?.let {
                it.checkEmailUnique(entity.email)
                entity.email = it
            }
            passwordHash?.let {
                if (entity.oauthType == null) {
                    user.currentPassword.checkAuth(entity.passwordHash)
                }
                entity.passwordHash = passwordHash
            }
            user.companyName?.let { entity.companyName = it.orNull() }
            user.companyLogoUrl?.let { entity.companyLogoUrl = it.orNull()?.asValidUrl() }
            user.companyIpRights?.let { entity.companyIpRights = it }
            user.distributionUserId?.let { entity.distributionUserId = it.orNull() }
            user.distributionArtistId?.let { entity.distributionArtistId = it }
            user.distributionParticipantId?.let { entity.distributionParticipantId = it }
            user.distributionSubscriptionId?.let { entity.distributionSubscriptionId = it }
            user.distributionLabelId?.let { entity.distributionLabelId = it }
            user.distributionIsni?.let { entity.distributionIsni = it.orNull() }
            user.distributionIpn?.let { entity.distributionIpn = it.orNull() }
        }
    }

    /**
     * Update data fields except for email, password, and oauth fields.
     */
    override fun updateUserData(userId: UUID, user: User) {
        transaction {
            val entity = UserEntity[userId]
            user.firstName?.let { entity.firstName = it.orNull() }
            user.lastName?.let { entity.lastName = it.orNull() }
            user.nickname?.let { entity.nickname = it.orNull() }
            user.pictureUrl?.let { entity.pictureUrl = it.orNull()?.asValidUrl() }
            user.bannerUrl?.let { entity.bannerUrl = it.asValidUrl() }
            user.websiteUrl?.let { entity.websiteUrl = it.asValidUrl() }
            user.twitterUrl?.let { entity.twitterUrl = it.asValidUrl() }
            user.instagramUrl?.let { entity.instagramUrl = it.asValidUrl() }
            user.spotifyProfile?.let { entity.spotifyProfile = it.orNull() }
            user.soundCloudProfile?.let { entity.soundCloudProfile = it.orNull() }
            user.appleMusicProfile?.let { entity.appleMusicProfile = it.orNull() }
            user.location?.let { entity.location = it }
            user.role?.let { entity.role = it }
            user.genre?.let { entity.genre = it }
            user.biography?.let { entity.biography = it }
            user.walletAddress?.let { entity.walletAddress = it }
            user.companyName?.let { entity.companyName = it }
            user.companyLogoUrl?.let { entity.companyLogoUrl = it.orNull()?.asValidUrl() }
            user.companyIpRights?.let { entity.companyIpRights = it }
            user.distributionUserId?.let { entity.distributionUserId = it }
            user.distributionArtistId?.let { entity.distributionArtistId = it }
            user.distributionParticipantId?.let { entity.distributionParticipantId = it }
            user.distributionSubscriptionId?.let { entity.distributionSubscriptionId = it }
            user.distributionLabelId?.let { entity.distributionLabelId = it }
            user.distributionIsni?.let { entity.distributionIsni = it.orNull() }
            user.distributionIpn?.let { entity.distributionIpn = it.orNull() }
        }
    }

    override suspend fun recover(user: User) {
        logger.debug { "recover: user = $user" }

        val email = user.email.asValidEmail().asVerifiedEmail(user.authCode)
        val passwordHash = user.newPassword.asValidPassword(user.confirmPassword).toHash()

        transaction {
            getUserEntityByEmail(email).passwordHash = passwordHash
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

    private fun String.checkEmailUnique(currentEmail: String? = null) {
        if (!equals(currentEmail, ignoreCase = true) && UserEntity.existsByEmail(this)) {
            throw HttpConflictException("Already exists: $this")
        }
    }

    private fun Password?.checkAuth(hash: String?) {
        if (this == null) throw HttpBadRequestException("Missing password")
        if (hash == null || !verify(hash)) throw HttpUnauthorizedException("Invalid password")
    }

    private suspend fun User.checkWhitelist() {
        checkWhitelist(email!!)
    }

    private suspend fun OAuthUser.checkWhitelist() {
        checkWhitelist(email!!)
    }

    private suspend fun checkWhitelist(email: String) {
        if (configRepository.exists(CONFIG_KEY_EMAIL_WHITELIST)) {
            val whitelistRegexList = configRepository.getStrings(CONFIG_KEY_EMAIL_WHITELIST).map {
                Regex(it, RegexOption.IGNORE_CASE)
            }
            if (whitelistRegexList.none { it.matches(email) }) {
                throw HttpUnauthorizedException("Email not whitelisted: $email")
            }
        }
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
