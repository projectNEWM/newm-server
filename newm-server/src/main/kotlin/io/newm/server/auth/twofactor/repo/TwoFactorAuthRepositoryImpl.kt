package io.newm.server.auth.twofactor.repo

import io.ktor.server.application.ApplicationEnvironment
import io.ktor.util.logging.Logger
import io.newm.server.auth.twofactor.database.TwoFactorAuthEntity
import io.newm.shared.koin.inject
import io.newm.shared.ktx.debug
import io.newm.shared.ktx.getBoolean
import io.newm.shared.ktx.getConfigChild
import io.newm.shared.ktx.getInt
import io.newm.shared.ktx.getLong
import io.newm.shared.ktx.getString
import io.newm.shared.ktx.nextDigitCode
import io.newm.shared.ktx.toHash
import io.newm.shared.ktx.toUrl
import io.newm.shared.ktx.verify
import org.apache.commons.mail.DefaultAuthenticator
import org.apache.commons.mail.HtmlEmail
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.parameter.parametersOf
import java.security.SecureRandom
import java.time.LocalDateTime

internal class TwoFactorAuthRepositoryImpl(
    private val environment: ApplicationEnvironment
) : TwoFactorAuthRepository {

    private val logger: Logger by inject { parametersOf(javaClass.simpleName) }
    private val random = SecureRandom()

    override suspend fun sendCode(email: String) {
        logger.debug { "sendCode: $email" }

        val config = environment.getConfigChild("emailAuth")
        val code = random.nextDigitCode(config.getInt("codeSize"))
        val message = config.getString("messageUrl")
            .toUrl()
            .readText()
            .replaceFirst("{{code}}", code)

        HtmlEmail().apply {
            hostName = config.getString("smtpHost")
            setSmtpPort(config.getInt("smtpPort"))
            isSSLOnConnect = config.getBoolean("sslOnConnect")
            setAuthenticator(
                DefaultAuthenticator(
                    config.getString("userName"),
                    config.getString("password")
                )
            )
            setFrom(config.getString("from"))
            addTo(email)
            subject = config.getString("subject")
            setHtmlMsg(message)
        }.send()

        val codeHash = code.toHash()
        val expiresAt = LocalDateTime.now().plusSeconds(config.getLong("timeToLive"))
        transaction {
            TwoFactorAuthEntity.deleteByEmail(email)
            TwoFactorAuthEntity.new {
                this.email = email
                this.codeHash = codeHash
                this.expiresAt = expiresAt
            }
        }
    }

    override suspend fun verifyCode(email: String, code: String): Boolean = transaction {
        TwoFactorAuthEntity.deleteAllExpired()
        TwoFactorAuthEntity.getByEmail(email)?.takeIf { code.verify(it.codeHash) }?.let { entity ->
            entity.delete()
            true
        } ?: false
    }
}
