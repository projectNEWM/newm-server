package io.newm.server.auth.twofactor.repo

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.ApplicationEnvironment
import io.newm.server.auth.twofactor.database.TwoFactorAuthEntity
import io.newm.server.features.email.repo.EmailRepository
import io.newm.server.features.user.database.UserEntity
import io.newm.shared.ktx.getConfigChild
import io.newm.shared.ktx.getInt
import io.newm.shared.ktx.getLong
import io.newm.shared.ktx.getString
import io.newm.shared.ktx.nextDigitCode
import io.newm.shared.ktx.toHash
import io.newm.shared.ktx.verify
import org.jetbrains.exposed.sql.transactions.transaction
import java.security.SecureRandom
import java.time.LocalDateTime

internal class TwoFactorAuthRepositoryImpl(
    private val environment: ApplicationEnvironment,
    private val emailRepository: EmailRepository
) : TwoFactorAuthRepository {
    private val logger = KotlinLogging.logger {}

    private val random = SecureRandom()

    override suspend fun sendCode(
        email: String,
        mustExists: Boolean,
        isMobileApp: Boolean
    ) {
        logger.debug { "sendCode: $email" }
        val exists = transaction { UserEntity.existsByEmail(email) }
        if (mustExists && !exists) {
            logger.warn { "Ignoring 2fa auth-code request for non-existing user $email" }
            return
        }
        val emailType = if (exists) "resetEmail" else "joinEmail"
        with(environment.getConfigChild("twoFactorAuth")) {
            val code = random.nextDigitCode(getInt("codeSize"))
            emailRepository.send(
                to = email,
                subject = getString("$emailType.subject"),
                messageUrl = getString("$emailType.messageUrl"),
                messageArgs = mapOf(
                    "code" to code,
                    "webAppDisplay" to if (isMobileApp) "none" else "inline",
                    "mobileAppDisplay" to if (isMobileApp) "inline" else "none"
                )
            )

            val codeHash = code.toHash()
            val expiresAt = LocalDateTime.now().plusSeconds(getLong("timeToLive"))
            transaction {
                TwoFactorAuthEntity.deleteByEmail(email)
                TwoFactorAuthEntity.new {
                    this.email = email
                    this.codeHash = codeHash
                    this.expiresAt = expiresAt
                }
            }
        }
    }

    override suspend fun verifyCode(
        email: String,
        code: String
    ): Boolean =
        transaction {
            TwoFactorAuthEntity.deleteAllExpired()
            TwoFactorAuthEntity.getByEmail(email)?.takeIf { code.verify(it.codeHash) }?.let { entity ->
                entity.delete()
                true
            } ?: false
        }
}
