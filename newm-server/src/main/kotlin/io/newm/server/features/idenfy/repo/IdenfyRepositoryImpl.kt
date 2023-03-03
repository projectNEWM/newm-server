package io.newm.server.features.idenfy.repo

import io.ktor.server.application.ApplicationEnvironment
import io.newm.server.ext.*
import io.newm.server.features.idenfy.model.IdenfyRequest
import io.newm.server.features.user.database.UserEntity
import io.newm.server.features.user.model.UserVerificationStatus
import org.apache.commons.mail.DefaultAuthenticator
import org.apache.commons.mail.HtmlEmail
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.Logger
import org.slf4j.MarkerFactory
import java.util.Properties

class IdenfyRepositoryImpl(
    private val environment: ApplicationEnvironment,
    private val logger: Logger
) : IdenfyRepository {

    private val marker = MarkerFactory.getMarker(javaClass.simpleName)
    private val messages: Properties by lazy {
        propertiesFromResource("idenfy-messages.properties")
    }

    override suspend fun processRequest(request: IdenfyRequest) {
        logger.debug(marker, "processRequest: $request")

        val status = when {
            request.isApproved -> UserVerificationStatus.Verified
            !request.isFinal -> UserVerificationStatus.Pending
            else -> UserVerificationStatus.Unverified
        }
        logger.debug(marker, "processRequest: status = $status")

        val email = transaction {
            with(UserEntity[request.clientId.toUUID()]) {
                verificationStatus = status
                email
            }
        }

        val config = environment.getConfigChild("idenfy.email")
        if (!config.getBoolean("enabled")) return

        val message = when (status) {
            UserVerificationStatus.Verified -> {
                config.getString("verifiedMessageUrl")
                    .toUrl()
                    .readText()
            }

            UserVerificationStatus.Pending -> {
                config.getString("pendingMessageUrl")
                    .toUrl()
                    .readText()
            }

            UserVerificationStatus.Unverified -> {
                val codes = mutableListOf<String>()
                with(request.status) {
                    autoDocument?.let { codes += it }
                    autoFace?.let { codes += it }
                    manualDocument?.let { codes += it }
                    manualFace?.let { codes += it }
                    mismatchTags?.let { codes += it }
                    suspicionReasons?.let { codes += it }
                }
                logger.debug(marker, "processRequest: codes = $codes")
                val reasons = codes.joinToString(separator = "<br/><br/>") { code ->
                    "&bull; ${messages.getProperty(code, code)}"
                }
                config.getString("unverifiedMessageUrl")
                    .toUrl()
                    .readText()
                    .replaceFirst("{{reasons}}", reasons)
            }
        }

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
    }
}
