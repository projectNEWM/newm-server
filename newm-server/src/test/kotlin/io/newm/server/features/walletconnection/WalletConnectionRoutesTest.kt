package io.newm.server.features.walletconnection

import com.google.common.truth.Truth
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.http.contentType
import io.ktor.utils.io.core.toByteArray
import io.newm.chain.util.toHexString
import io.newm.server.BaseApplicationTests
import io.newm.server.features.user.database.UserEntity
import io.newm.server.features.user.database.UserTable
import io.newm.server.features.walletconnection.database.WalletConnectionChallengeEntity
import io.newm.server.features.walletconnection.database.WalletConnectionChallengeTable
import io.newm.server.features.walletconnection.database.WalletConnectionEntity
import io.newm.server.features.walletconnection.database.WalletConnectionTable
import io.newm.server.features.walletconnection.model.AnswerChallengeRequest
import io.newm.server.features.walletconnection.model.AnswerChallengeResponse
import io.newm.server.features.walletconnection.model.ChallengeMethod
import io.newm.server.features.walletconnection.model.ConnectResponse
import io.newm.server.features.walletconnection.model.GenerateChallengeRequest
import io.newm.server.features.walletconnection.model.GenerateChallengeResponse
import io.newm.shared.ktx.existsHavingId
import io.newm.shared.ktx.toTempFile
import kotlinx.coroutines.runBlocking
import org.apache.tika.Tika
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

private const val TEST_STAKE_ADDRESS = "stake_test17rd6aqx9mutz9r24ttk2ezwvel9tgf9sp7s389rexjgk9kssedugy"

class WalletConnectionRoutesTest : BaseApplicationTests() {
    @BeforeEach
    fun beforeEach() {
        transaction {
            WalletConnectionChallengeTable.deleteAll()
            WalletConnectionTable.deleteAll()
        }
    }

    @Test
    fun testGenerateSignedDataChallenge() =
        runBlocking {
            val startTime = LocalDateTime.now()
            val response =
                client.post("v1/wallet-connections/challenges/generate") {
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    setBody(
                        GenerateChallengeRequest(
                            method = ChallengeMethod.SignedData,
                            stakeAddress = TEST_STAKE_ADDRESS
                        )
                    )
                }
            Truth.assertThat(response.status).isEqualTo(HttpStatusCode.OK)

            // verify database values
            val entity =
                transaction {
                    WalletConnectionChallengeEntity.all().first()
                }

            Truth.assertThat(entity.createdAt).isAtLeast(startTime)
            Truth.assertThat(entity.method).isEqualTo(ChallengeMethod.SignedData)
            Truth.assertThat(entity.stakeAddress).isEqualTo(TEST_STAKE_ADDRESS)
            val expectedPayload = """{"connectTo":"NEWM Mobile ${entity.id.value}","stakeAddress":"$TEST_STAKE_ADDRESS"}""".toByteArray().toHexString()
            Truth.assertThat(entity.payload).isEqualTo(expectedPayload)

            // verify response body values
            val body = response.body<GenerateChallengeResponse>()
            Truth.assertThat(body.challengeId).isEqualTo(entity.id.value)
            Truth.assertThat(body.expiresAt).isEqualTo(entity.createdAt.plusSeconds(60))
            Truth.assertThat(body.payload).isEqualTo(expectedPayload)
        }

    @Test
    @Disabled
    fun testGenerateSignedTransactionChallenge() {
        // TODO
    }

    @Test
    fun testAnswerSignedDataChallenge() =
        runBlocking {
            val startTime = LocalDateTime.now()
            val challengeId =
                transaction {
                    WalletConnectionChallengeEntity.new {
                        method = ChallengeMethod.SignedData
                        stakeAddress = TEST_STAKE_ADDRESS
                        payload = "NOTHING"
                    }.id.value
                }

            val response =
                client.post("v1/wallet-connections/challenges/answer") {
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    setBody(
                        AnswerChallengeRequest(
                            challengeId = challengeId,
                            payload = "TODO"
                        )
                    )
                }
            Truth.assertThat(response.status).isEqualTo(HttpStatusCode.OK)

            // verify database values
            val entity =
                transaction {
                    WalletConnectionEntity.all().first()
                }
            Truth.assertThat(entity.createdAt).isAtLeast(startTime)
            Truth.assertThat(entity.stakeAddress).isEqualTo(TEST_STAKE_ADDRESS)
            Truth.assertThat(entity.userId).isNull()

            // verify response body values
            val body = response.body<AnswerChallengeResponse>()
            Truth.assertThat(body.connectionId).isEqualTo(entity.id.value)
            Truth.assertThat(body.expiresAt).isEqualTo(entity.createdAt.plusSeconds(300))
        }

    @Test
    @Disabled
    fun testAnswerSignedTransactionChallenge() {
        // TODO
    }

    @Test
    fun testGenerateQRCode() =
        runBlocking {
            val connectionId =
                transaction {
                    WalletConnectionEntity.new {
                        stakeAddress = TEST_STAKE_ADDRESS
                    }.id.value
                }

            val response =
                client.get("v1/wallet-connections/$connectionId/qrcode") {
                    accept(ContentType.Image.Any)
                }
            Truth.assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            Truth.assertThat(response.contentType()).isEqualTo(ContentType.Image.PNG)
            Truth.assertThat(response.contentLength()).isAtLeast(30000L)

            // verify content
            val file = response.bodyAsChannel().toTempFile()
            try {
                val length = file.length()
                Truth.assertThat(length).isEqualTo(response.contentLength())
                val type = Tika().detect(file)
                Truth.assertThat(type).isEqualTo(ContentType.Image.PNG.toString())
            } finally {
                file.delete()
            }
        }

    @Test
    fun testConnectWalletFromMobile() =
        runBlocking {
            val userId =
                transaction {
                    UserEntity.new {
                        email = "testuser@newm.io"
                    }.id.value
                }
            val connectionId =
                transaction {
                    WalletConnectionEntity.new {
                        stakeAddress = TEST_STAKE_ADDRESS
                    }.id.value
                }

            val response =
                client.get("v1/wallet-connections/$connectionId") {
                    bearerAuth(userId.toString())
                    accept(ContentType.Application.Json)
                }
            Truth.assertThat(response.status).isEqualTo(HttpStatusCode.OK)

            // verify database values
            val entity =
                transaction {
                    WalletConnectionEntity[connectionId]
                }
            Truth.assertThat(entity.userId?.value).isEqualTo(userId)

            // verify response body values
            val body = response.body<ConnectResponse>()
            Truth.assertThat(body.connectionId).isEqualTo(connectionId)
            Truth.assertThat(body.stakeAddress).isEqualTo(entity.stakeAddress)
        }

    @Test
    fun testDisconnectWalletFromMobile() =
        runBlocking {
            val userId =
                transaction {
                    UserEntity.new {
                        email = "testuser@newm.io"
                    }.id.value
                }
            val connectionId =
                transaction {
                    WalletConnectionEntity.new {
                        stakeAddress = TEST_STAKE_ADDRESS
                        this.userId = EntityID(userId, UserTable)
                    }.id.value
                }

            val response =
                client.delete("v1/wallet-connections/$connectionId") {
                    bearerAuth(userId.toString())
                }
            Truth.assertThat(response.status).isEqualTo(HttpStatusCode.NoContent)

            // verify that it's gone in the database
            val exists = transaction { WalletConnectionEntity.existsHavingId(userId) }
            Truth.assertThat(exists).isFalse()
        }
}
