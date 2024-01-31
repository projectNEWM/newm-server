package io.newm.server.auth.twofactor.repo

import io.ktor.server.application.ApplicationEnvironment
import io.ktor.util.logging.Logger
import io.newm.server.auth.twofactor.database.TwoFactorAuthEntity
import io.newm.server.features.email.repo.EmailRepository
import io.newm.server.features.user.database.UserEntity
import io.newm.shared.koin.inject
import io.newm.shared.ktx.debug
import io.newm.shared.ktx.getConfigChild
import io.newm.shared.ktx.getInt
import io.newm.shared.ktx.getLong
import io.newm.shared.ktx.getString
import io.newm.shared.ktx.nextDigitCode
import io.newm.shared.ktx.toHash
import io.newm.shared.ktx.verify
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.parameter.parametersOf
import java.security.SecureRandom
import java.time.LocalDateTime

internal class TwoFactorAuthRepositoryImpl(
    private val environment: ApplicationEnvironment,
    private val emailRepository: EmailRepository
) : TwoFactorAuthRepository {
    private val logger: Logger by inject { parametersOf(javaClass.simpleName) }
    private val random = SecureRandom()

    override suspend fun sendCode(email: String) {
        logger.debug { "sendCode: $email" }
        val emailType = if (transaction { UserEntity.existsByEmail(email) }) "resetEmail" else "joinEmail"
        with(environment.getConfigChild("twoFactorAuth")) {
            val code = random.nextDigitCode(getInt("codeSize"))
            emailRepository.send(
                to = email,
                subject = getString("$emailType.subject"),
                messageUrl = getString("$emailType.messageUrl"),
                messageArgs = mapOf("code" to code)
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
