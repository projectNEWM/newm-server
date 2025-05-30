package io.newm.server.features.idenfy

import com.google.common.truth.Truth.assertThat
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.newm.server.BaseApplicationTests
import io.newm.server.features.idenfy.model.IdenfyCreateSessionResponse
import io.newm.server.features.idenfy.model.IdenfySessionResult
import io.newm.server.features.user.database.UserEntity
import io.newm.server.features.user.model.UserVerificationStatus
import io.newm.shared.koin.inject
import io.newm.shared.ktx.toHexString
import java.lang.System.currentTimeMillis
import java.security.Key
import javax.crypto.Mac
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test

class IdenfyRoutesTests : BaseApplicationTests() {
    @Test
    fun testCreateSession() =
        runBlocking {
            val response =
                client.get("v1/idenfy/session") {
                    bearerAuth(testUserToken)
                }
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val resp = response.body<IdenfyCreateSessionResponse>()
            assertThat(resp.authToken).isEqualTo(testUserId.toString())
            assertThat(resp.expiryTime).isEqualTo(testUserId.toString().hashCode())
        }

    @Test
    fun testSessionResultVerified() =
        runBlocking {
            val json: Json by inject()
            val key: Key by inject(IDENFY_KEY_QUALIFIER)

            transaction {
                UserEntity[testUserId].verificationStatus = UserVerificationStatus.Unverified
            }

            val t = currentTimeMillis()
            val docFirstName = "DocFirstName@$t"
            val docLastName = "DocLastName@$t"
            val selectedCountry = "SelectedCountry@$t"
            val docIssuingCountry = "DocIssuingCountry@$t"
            val docNationality = "DocNationality@$t"
            val orgNationality = "OrgNationality@$t"
            val request =
                json.encodeToString(
                    IdenfySessionResult(
                        clientId = testUserId.toString(),
                        isFinal = false,
                        status =
                            IdenfySessionResult.Status(
                                overall = "APPROVED",
                                autoDocument = null,
                                autoFace = null,
                                fraudTags = null,
                                manualDocument = null,
                                manualFace = null,
                                mismatchTags = null,
                                suspicionReasons = null
                            ),
                        data =
                            IdenfySessionResult.Data(
                                docFirstName = docFirstName,
                                docLastName = docLastName,
                                selectedCountry = selectedCountry,
                                docIssuingCountry = docIssuingCountry,
                                docNationality = docNationality,
                                orgNationality = orgNationality,
                            )
                    )
                )

            val response =
                client.post("v1/idenfy/callback") {
                    contentType(ContentType.Application.Json)
                    header("Idenfy-Signature", request.sign(key))
                    setBody(request)
                }
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)

            val user = transaction { UserEntity[testUserId] }
            assertThat(user.verificationStatus).isEqualTo(UserVerificationStatus.Verified)
            assertThat(user.firstName).isEqualTo(docFirstName)
            assertThat(user.lastName).isEqualTo(docLastName)
        }

    @Test
    fun testSessionResultPending() =
        runBlocking {
            val json: Json by inject()
            val key: Key by inject(IDENFY_KEY_QUALIFIER)

            transaction {
                UserEntity[testUserId].verificationStatus = UserVerificationStatus.Unverified
            }

            val request =
                json.encodeToString(
                    IdenfySessionResult(
                        clientId = testUserId.toString(),
                        isFinal = false,
                        status =
                            IdenfySessionResult.Status(
                                overall = "REVIEWING",
                                autoDocument = null,
                                autoFace = null,
                                fraudTags = null,
                                manualDocument = null,
                                manualFace = null,
                                mismatchTags = null,
                                suspicionReasons = null
                            ),
                        data =
                            IdenfySessionResult.Data(
                                docFirstName = null,
                                docLastName = null,
                                selectedCountry = null,
                                docIssuingCountry = null,
                                docNationality = null,
                                orgNationality = null,
                            )
                    )
                )

            val response =
                client.post("v1/idenfy/callback") {
                    contentType(ContentType.Application.Json)
                    header("Idenfy-Signature", request.sign(key))
                    setBody(request)
                }
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)

            val newStatus =
                transaction {
                    UserEntity[testUserId].verificationStatus
                }
            assertThat(newStatus).isEqualTo(UserVerificationStatus.Pending)
        }

    @Test
    fun testSessionResultUnverified() =
        runBlocking {
            val json: Json by inject()
            val key: Key by inject(IDENFY_KEY_QUALIFIER)

            transaction {
                UserEntity[testUserId].verificationStatus = UserVerificationStatus.Pending
            }

            val request =
                json.encodeToString(
                    IdenfySessionResult(
                        clientId = testUserId.toString(),
                        isFinal = true,
                        status =
                            IdenfySessionResult.Status(
                                overall = "DENIED",
                                autoDocument = null,
                                autoFace = null,
                                fraudTags = null,
                                manualDocument = null,
                                manualFace = null,
                                mismatchTags = null,
                                suspicionReasons = null
                            ),
                        data =
                            IdenfySessionResult.Data(
                                docFirstName = null,
                                docLastName = null,
                                selectedCountry = null,
                                docIssuingCountry = null,
                                docNationality = null,
                                orgNationality = null,
                            )
                    )
                )

            val response =
                client.post("v1/idenfy/callback") {
                    contentType(ContentType.Application.Json)
                    header("Idenfy-Signature", request.sign(key))
                    setBody(request)
                }
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)

            val newStatus =
                transaction {
                    UserEntity[testUserId].verificationStatus
                }
            assertThat(newStatus).isEqualTo(UserVerificationStatus.Unverified)
        }
}

private fun String.sign(key: Key): String =
    with(Mac.getInstance(key.algorithm)) {
        init(key)
        doFinal(toByteArray()).toHexString()
    }
