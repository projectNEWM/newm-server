package io.newm.server.auth.twofactor.repo

import io.ktor.server.application.ApplicationEnvironment
import io.ktor.util.logging.Logger
import io.newm.server.auth.twofactor.database.TwoFactorAuthEntity
import io.newm.server.ext.*
import org.apache.commons.mail.DefaultAuthenticator
import org.apache.commons.mail.HtmlEmail
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.MarkerFactory
import java.security.SecureRandom
import java.time.LocalDateTime

internal class TwoFactorAuthRepositoryImpl(
    private val environment: ApplicationEnvironment,
    private val logger: Logger
) : TwoFactorAuthRepository {

    private val marker = MarkerFactory.getMarker(javaClass.simpleName)
    private val random = SecureRandom()

    override suspend fun sendCode(email: String) {
        logger.debug(marker, "sendCode: $email")

        val code = random.nextDigitCode(environment.getConfigInt("emailAuth.codeSize"))

        val message = environment.getConfigString("emailAuth.messageUrl")
            .toUrl()
            .readText()
            .replaceFirst("{{code}}", code)

        val config = environment.getConfigChild("emailAuth")
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
