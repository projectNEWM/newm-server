package io.newm.server.features.idenfy.repo

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.ApplicationEnvironment
import io.newm.server.features.idenfy.model.IdenfyCreateSessionRequest
import io.newm.server.features.idenfy.model.IdenfyCreateSessionResponse
import io.newm.server.features.idenfy.model.IdenfySessionResult
import io.newm.server.features.user.database.UserEntity
import io.newm.server.features.user.model.UserVerificationStatus
import io.newm.shared.ext.*
import io.newm.shared.koin.inject
import org.apache.commons.mail.DefaultAuthenticator
import org.apache.commons.mail.HtmlEmail
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.parameter.parametersOf
import org.slf4j.Logger
import java.util.*

class IdenfyRepositoryImpl(
    private val environment: ApplicationEnvironment,
    private val httpClient: HttpClient
) : IdenfyRepository {

    private val logger: Logger by inject { parametersOf(javaClass.simpleName) }
    private val messages: Properties by lazy {
        propertiesFromResource("idenfy-messages.properties")
    }

    override suspend fun createSession(userId: UUID): IdenfyCreateSessionResponse {
        logger.debug { "createSession: $userId" }

        return with(environment.getConfigChild("idenfy")) {
            httpClient.post(getString("sessionUrl")) {
                contentType(ContentType.Application.Json)
                basicAuth(
                    username = getString("apiKey"),
                    password = getString("apiSecret")
                )
                setBody(IdenfyCreateSessionRequest(userId.toString()))
            }.body()
        }
    }

    override suspend fun processSessionResult(result: IdenfySessionResult) {
        logger.debug { "processSessionResult: $result" }

        val status = when {
            result.isApproved -> UserVerificationStatus.Verified
            !result.isFinal -> UserVerificationStatus.Pending
            else -> UserVerificationStatus.Unverified
        }
        logger.debug { "processRequest: status = $status" }

        val email = transaction {
            with(UserEntity[result.clientId.toUUID()]) {
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
                with(result.status) {
                    autoDocument?.let { codes += it }
                    autoFace?.let { codes += it }
                    manualDocument?.let { codes += it }
                    manualFace?.let { codes += it }
                    mismatchTags?.let { codes += it }
                    suspicionReasons?.let { codes += it }
                }
                logger.debug { "processRequest: codes = $codes" }
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
