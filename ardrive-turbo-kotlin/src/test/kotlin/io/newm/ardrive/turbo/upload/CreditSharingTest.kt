package io.newm.ardrive.turbo.upload

import com.google.common.truth.Truth.assertThat
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.newm.ardrive.turbo.TurboConfig
import io.newm.ardrive.turbo.auth.ArweaveSigner
import io.newm.ardrive.turbo.upload.model.DataItemOptions
import io.newm.ardrive.turbo.upload.model.DataItemTag
import io.newm.ardrive.turbo.upload.util.TestJwkFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class CreditSharingTest {
    @Test
    fun `shareCredits uploads approval tags`() =
        runBlocking {
            val engine = MockEngine { request ->
                assertThat(request.method).isEqualTo(HttpMethod.Post)
                assertThat(request.headers["x-public-key"]).isNotEmpty()
                assertThat(request.headers["x-nonce"]).isNotEmpty()
                assertThat(request.headers["x-signature"]).isNotEmpty()
                respond(
                    content =
                        """
                        {
                          "dataCaches":[],
                          "fastFinalityIndexes":[],
                          "id":"id",
                          "owner":"owner",
                          "winc":"1",
                          "createdApproval":{
                            "approvalDataItemId":"approval",
                            "approvedAddress":"addr",
                            "payingAddress":"payer",
                            "approvedWincAmount":"100",
                            "usedWincAmount":"0",
                            "creationDate":"now",
                            "expirationDate":null
                          }
                        }
                        """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = io.ktor.http.headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
            val client = HttpClient(engine) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            }
            val service = UploadServiceImpl(
                config = TurboConfig(uploadBaseUrl = "https://upload.ardrive.io"),
                signer = ArweaveSigner(TestJwkFactory.create()),
                httpClient = client,
            )

            val approval = service.shareCredits(
                approvedAddress = "addr",
                approvedWincAmount = "100",
                expiresBySeconds = 30,
            )

            assertThat(approval.approvedAddress).isEqualTo("addr")
        }

    @Test
    fun `revokeCredits uploads revoke tags`() =
        runBlocking {
            val engine = MockEngine { request ->
                assertThat(request.method).isEqualTo(HttpMethod.Post)
                respond(
                    content =
                        """
                        {
                          "dataCaches":[],
                          "fastFinalityIndexes":[],
                          "id":"id",
                          "owner":"owner",
                          "winc":"1",
                          "revokedApprovals":[
                            {
                              "approvalDataItemId":"approval",
                              "approvedAddress":"addr",
                              "payingAddress":"payer",
                              "approvedWincAmount":"100",
                              "usedWincAmount":"0",
                              "creationDate":"now",
                              "expirationDate":null
                            }
                          ]
                        }
                        """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = io.ktor.http.headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
            val client = HttpClient(engine) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            }
            val service = UploadServiceImpl(
                config = TurboConfig(uploadBaseUrl = "https://upload.ardrive.io"),
                signer = ArweaveSigner(TestJwkFactory.create()),
                httpClient = client,
            )

            val approvals = service.revokeCredits("addr")

            assertThat(approvals).hasSize(1)
            assertThat(approvals.first().approvedAddress).isEqualTo("addr")
        }
}
