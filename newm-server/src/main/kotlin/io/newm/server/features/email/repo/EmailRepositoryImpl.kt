package io.newm.server.features.email.repo

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.*
import io.newm.server.ktx.getSecureString
import io.newm.shared.ktx.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.apache.commons.mail.DefaultAuthenticator
import org.apache.commons.mail.HtmlEmail

internal class EmailRepositoryImpl(
    private val environment: ApplicationEnvironment
) : EmailRepository {
    private val logger = KotlinLogging.logger {}

    override suspend fun send(
        to: String,
        subject: String,
        messageUrl: String,
        messageArgs: Map<String, Any>
    ) = send(listOf(to), emptyList(), subject, messageUrl, messageArgs)

    override suspend fun send(
        to: List<String>,
        bcc: List<String>,
        subject: String,
        messageUrl: String,
        messageArgs: Map<String, Any>
    ): Unit =
        coroutineScope {
            launch {
                logger.debug { "send to: $to, bcc: $bcc, subject: $subject" }

                try {
                    with(environment.getConfigChild("email")) {
                        if (getBoolean("enabled")) {
                            val message =
                                messageUrl
                                    .toUrl()
                                    .readText()
                                    .format(messageArgs + getChild("arguments").toMap())

                            HtmlEmail().apply {
                                hostName = getSecureString("smtpHost")
                                setSmtpPort(getInt("smtpPort"))
                                isSSLOnConnect = getBoolean("sslOnConnect")
                                authenticator =
                                    DefaultAuthenticator(
                                        getSecureString("userName"),
                                        getSecureString("password")
                                    )
                                setFrom(getSecureString("from"))
                                to.forEach { addTo(it) }
                                bcc.forEach { addBcc(it) }
                                this.subject = subject
                                setHtmlMsg(message)
                            }.send()

                            logger.info { "Email sent to: $to, bcc: $bcc, subject: $subject" }
                        } else {
                            logger.warn { "Email notifications disabled - skipped sending message" }
                        }
                    }
                } catch (error: Throwable) {
                    logger.error(error) { "Failure sending to: $to, bcc: $bcc, subject: $subject" }
                }
            }
        }
}
