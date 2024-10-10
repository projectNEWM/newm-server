package io.newm.server.features.walletconnection

import com.google.common.truth.Truth
import com.google.protobuf.ByteString
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
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.newm.chain.grpc.TransactionBuilderResponse
import io.newm.chain.grpc.verifySignDataResponse
import io.newm.server.BaseApplicationTests
import io.newm.server.features.cardano.repo.CardanoRepository
import io.newm.server.features.user.database.UserEntity
import io.newm.server.features.user.database.UserTable
import io.newm.server.features.walletconnection.database.WalletConnectionChallengeEntity
import io.newm.server.features.walletconnection.database.WalletConnectionChallengeTable
import io.newm.server.features.walletconnection.database.WalletConnectionEntity
import io.newm.server.features.walletconnection.database.WalletConnectionTable
import io.newm.server.features.walletconnection.model.AnswerChallengeRequest
import io.newm.server.features.walletconnection.model.AnswerChallengeResponse
import io.newm.server.features.walletconnection.model.ChallengeMethod
import io.newm.server.features.walletconnection.model.WalletConnection
import io.newm.server.features.walletconnection.model.GenerateChallengeRequest
import io.newm.server.features.walletconnection.model.GenerateChallengeResponse
import io.newm.shared.ktx.existsHavingId
import io.newm.shared.ktx.toHexString
import io.newm.shared.ktx.toTempFile
import kotlinx.coroutines.runBlocking
import org.apache.tika.Tika
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.GlobalContext.loadKoinModules
import org.koin.dsl.module
import java.time.LocalDateTime
import java.util.UUID

private const val TEST_STAKE_ADDRESS = "stake_test1upfa42cuzftdzkg4pmfx80kqsln2vyymgedsz58fuwa5y6gjft7zv"
private const val TEST_CHANGE_ADDRESS =
    "addr_test1qqfam8majz3desfm82qvd28yzfrg4etfas4acf4kkggaq6slhc0vy4v22ml5mrq8q40wvj648xvyllld92w6lpk2y97q04cqhl"

class WalletConnectionRoutesTest : BaseApplicationTests() {
    private val cardanoRepository = mockk<CardanoRepository>(relaxed = true)

    @BeforeAll
    fun beforeAllLocal() {
        loadKoinModules(
            module {
                single { cardanoRepository }
            }
        )
    }

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

            // verify response body values
            val body = response.body<GenerateChallengeResponse>()
            Truth.assertThat(body.challengeId).isEqualTo(entity.id.value)
            Truth.assertThat(body.expiresAt).isEqualTo(entity.createdAt.plusSeconds(60))
            val expectedPayload = buildChallengeString(entity.id.value).toByteArray().toHexString()
            Truth.assertThat(body.payload).isEqualTo(expectedPayload)
        }

    @Test
    fun testAnswerSignedDataChallenge() =
        runBlocking {
            val startTime = LocalDateTime.now()
            val challengeId =
                transaction {
                    WalletConnectionChallengeEntity
                        .new {
                            method = ChallengeMethod.SignedData
                            stakeAddress = TEST_STAKE_ADDRESS
                        }.id.value
                }

            val expectedSignature = "7b22636f6e6e656374546f223a224e45574d204d6f62696c652066343238333539302d3461"
            val expectedKey = "d343865392d613933382d313366653864303836663735222c227374616b6541646472657373"
            coEvery { cardanoRepository.verifySignData(expectedSignature, expectedKey) } returns
                verifySignDataResponse {
                    verified = true
                    challenge = buildChallengeString(challengeId)
                }

            val response =
                client.post("v1/wallet-connections/challenges/answer") {
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    setBody(
                        AnswerChallengeRequest(
                            challengeId = challengeId,
                            payload = expectedSignature,
                            key = expectedKey
                        )
                    )
                }
            Truth.assertThat(response.status).isEqualTo(HttpStatusCode.OK)

            // verify database values
            val challengeExists = transaction { WalletConnectionChallengeEntity.existsHavingId(challengeId) }
            Truth.assertThat(challengeExists).isFalse()
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
    fun testGenerateSignedTransactionChallenge() =
        runBlocking {
            val expectedPayload = "7b22636f6e6e656374546f223a224e45574d204d6f62696c652066343238333539302d3461"
            val transactionBuilderResponse = mockk<TransactionBuilderResponse>(relaxed = true)
            every { transactionBuilderResponse.transactionCbor } returns ByteString.fromHex(expectedPayload)
            coEvery { cardanoRepository.buildTransaction(any()) } returns transactionBuilderResponse

            val startTime = LocalDateTime.now()
            val response =
                client.post("v1/wallet-connections/challenges/generate") {
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    setBody(
                        GenerateChallengeRequest(
                            method = ChallengeMethod.SignedTransaction,
                            stakeAddress = TEST_STAKE_ADDRESS,
                            changeAddress = TEST_CHANGE_ADDRESS,
                            utxoCborHexList = listOf("utxo1", "utxo2")
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
            Truth.assertThat(entity.method).isEqualTo(ChallengeMethod.SignedTransaction)
            Truth.assertThat(entity.stakeAddress).isEqualTo(TEST_STAKE_ADDRESS)

            // verify response body values
            val body = response.body<GenerateChallengeResponse>()
            Truth.assertThat(body.challengeId).isEqualTo(entity.id.value)
            Truth.assertThat(body.expiresAt).isEqualTo(entity.createdAt.plusSeconds(60))
            Truth.assertThat(body.payload).isEqualTo(expectedPayload)
        }

    @Test
    fun testAnswerSignedTransactionChallenge() =
        runBlocking {
            val startTime = LocalDateTime.now()
            val challengeId =
                transaction {
                    WalletConnectionChallengeEntity
                        .new {
                            method = ChallengeMethod.SignedTransaction
                            stakeAddress = TEST_STAKE_ADDRESS
                        }.id.value
                }

            val expectedSignature = "7b22636f6e6e656374546f223a224e45574d204d6f62696c652066343238333539302d3461"
            coEvery { cardanoRepository.verifySignTransaction(expectedSignature) } returns
                verifySignDataResponse {
                    verified = true
                    challenge = buildChallengeString(challengeId)
                }

            val response =
                client.post("v1/wallet-connections/challenges/answer") {
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    setBody(
                        AnswerChallengeRequest(
                            challengeId = challengeId,
                            payload = expectedSignature
                        )
                    )
                }
            Truth.assertThat(response.status).isEqualTo(HttpStatusCode.OK)

            // verify database values
            val challengeExists = transaction { WalletConnectionChallengeEntity.existsHavingId(challengeId) }
            Truth.assertThat(challengeExists).isFalse()
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
    fun testGenerateQRCode() =
        runBlocking {
            val connectionId =
                transaction {
                    WalletConnectionEntity
                        .new {
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
                    UserEntity
                        .new {
                            email = "testuser@newm.io"
                        }.id.value
                }
            val connectionId =
                transaction {
                    WalletConnectionEntity
                        .new {
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
            val connection = response.body<WalletConnection>()
            Truth.assertThat(connection.id).isEqualTo(connectionId)
            Truth.assertThat(connection.createdAt).isEqualTo(entity.createdAt)
            Truth.assertThat(connection.stakeAddress).isEqualTo(entity.stakeAddress)
        }

    @Test
    fun testDisconnectWalletFromMobile() =
        runBlocking {
            val userId =
                transaction {
                    UserEntity
                        .new {
                            email = "testuser@newm.io"
                        }.id.value
                }
            val connectionId =
                transaction {
                    WalletConnectionEntity
                        .new {
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

    @Test
    fun testGetWalletConnectionsFromMobile() =
        runBlocking {
            val userId =
                transaction {
                    UserEntity
                        .new {
                            email = "testuser@newm.io"
                        }.id
                }
            val expectedConnections = mutableListOf<WalletConnection>()
            for (i in 0..4) {
                expectedConnections +=
                    transaction {
                        WalletConnectionEntity.new {
                            stakeAddress = TEST_STAKE_ADDRESS + i
                            this.userId = userId
                        }
                    }.toModel()
            }

            val response =
                client.get("v1/wallet-connections") {
                    bearerAuth(userId.toString())
                    accept(ContentType.Application.Json)
                }
            Truth.assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val actualConnections = response.body<List<WalletConnection>>()
            Truth.assertThat(actualConnections).isEqualTo(expectedConnections)
        }

    private fun buildChallengeString(challengeId: UUID): String =
        """{"connectTo":"NEWM Mobile $challengeId","stakeAddress":"$TEST_STAKE_ADDRESS"}"""
}
