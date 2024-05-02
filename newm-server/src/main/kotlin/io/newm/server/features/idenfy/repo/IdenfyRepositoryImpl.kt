package io.newm.server.features.idenfy.repo

import io.ktor.client.HttpClient
import io.ktor.client.request.basicAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.application.ApplicationEnvironment
import io.newm.server.features.email.repo.EmailRepository
import io.newm.server.features.idenfy.model.IdenfyCreateSessionRequest
import io.newm.server.features.idenfy.model.IdenfyCreateSessionResponse
import io.newm.server.features.idenfy.model.IdenfySessionResult
import io.newm.server.features.user.database.UserEntity
import io.newm.server.features.user.model.UserVerificationStatus
import io.newm.server.ktx.checkedBody
import io.newm.server.ktx.getSecureString
import io.newm.server.typealiases.UserId
import io.newm.shared.koin.inject
import io.newm.shared.ktx.debug
import io.newm.shared.ktx.getConfigChild
import io.newm.shared.ktx.getString
import io.newm.shared.ktx.orNull
import io.newm.shared.ktx.propertiesFromResource
import io.newm.shared.ktx.sanitizeName
import io.newm.shared.ktx.toUUID
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.parameter.parametersOf
import org.slf4j.Logger
import java.util.Properties
import java.util.UUID

class IdenfyRepositoryImpl(
    private val environment: ApplicationEnvironment,
    private val httpClient: HttpClient,
    private val emailRepository: EmailRepository,
) : IdenfyRepository {
    private val logger: Logger by inject { parametersOf(javaClass.simpleName) }
    private val messages: Properties by lazy {
        propertiesFromResource("idenfy-messages.properties")
    }

    override suspend fun createSession(userId: UserId): IdenfyCreateSessionResponse {
        logger.debug { "createSession: $userId" }

        return with(environment.getConfigChild("idenfy")) {
            httpClient.post(getString("sessionUrl")) {
                contentType(ContentType.Application.Json)
                basicAuth(
                    username = getSecureString("apiKey"),
                    password = getSecureString("apiSecret"),
                )
                setBody(
                    IdenfyCreateSessionRequest(
                        clientId = userId.toString(),
                        successUrl = getString("successUrl"),
                        errorUrl = getString("errorUrl")
                    )
                )
            }.checkedBody()
        }
    }

    override suspend fun processSessionResult(result: IdenfySessionResult) {
        logger.debug { "processSessionResult: $result" }

        val status =
            when {
                result.isApproved -> UserVerificationStatus.Verified
                !result.isFinal -> UserVerificationStatus.Pending
                else -> UserVerificationStatus.Unverified
            }
        logger.debug { "processRequest: status = $status" }

        val email =
            transaction {
                with(UserEntity[result.clientId.toUUID()]) {
                    verificationStatus = status
                    if (status == UserVerificationStatus.Verified) {
                        result.data.docFirstName?.let { firstName = it.sanitizeName() }
                        result.data.docLastName?.let { lastName = it.sanitizeName() }
                        (
                            result.data.selectedCountry?.orNull() ?: result.data.docIssuingCountry?.orNull()
                                ?: result.data.docNationality?.orNull() ?: result.data.orgNationality?.orNull()
                        )?.let { location = it.take(2) }
                    }
                    email
                }
            }

        val messageArgs = mutableMapOf<String, Any>()
        if (status == UserVerificationStatus.Unverified) {
            val codes = mutableSetOf<String>()
            with(result.status) {
                autoDocument?.let { codes += it }
                autoFace?.let { codes += it }
                manualDocument?.let { codes += it }
                manualFace?.let { codes += it }
                mismatchTags?.let { codes += it }
                suspicionReasons?.let { codes += it }
            }
            logger.debug { "processRequest: codes = $codes" }
            messageArgs += "reasons" to
                codes.joinToString(separator = "<br>") { code ->
                    "<li>${messages.getProperty(code, code)}</li>"
                }
        }

        with(environment.getConfigChild("idenfy.${status.name.lowercase()}Email")) {
            emailRepository.send(
                to = email,
                subject = getString("subject"),
                messageUrl = getString("messageUrl"),
                messageArgs = messageArgs
            )
        }
    }
}
