package io.projectnewm.server.auth.twofactor.repo

import io.ktor.server.application.ApplicationEnvironment
import io.ktor.util.logging.Logger
import io.projectnewm.server.auth.twofactor.database.TwoFactorAuthEntity
import io.projectnewm.server.ext.getConfigBoolean
import io.projectnewm.server.ext.getConfigInt
import io.projectnewm.server.ext.getConfigLong
import io.projectnewm.server.ext.getConfigString
import io.projectnewm.server.ext.nextDigitCode
import io.projectnewm.server.ext.toHash
import io.projectnewm.server.ext.toUrl
import io.projectnewm.server.ext.verify
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

        HtmlEmail().apply {
            hostName = environment.getConfigString("emailAuth.smtpHost")
            setSmtpPort(environment.getConfigInt("emailAuth.smtpPort"))
            isSSLOnConnect = environment.getConfigBoolean("emailAuth.sslOnConnect")
            setAuthenticator(
                DefaultAuthenticator(
                    environment.getConfigString("emailAuth.userName"),
                    environment.getConfigString("emailAuth.password")
                )
            )
            setFrom(environment.getConfigString("emailAuth.from"))
            addTo(email)
            subject = environment.getConfigString("emailAuth.subject")
            setHtmlMsg(message)
        }.send()

        val codeHash = code.toHash()
        val expiresAt = LocalDateTime.now().plusMinutes(environment.getConfigLong("emailAuth.timeToLive"))
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
