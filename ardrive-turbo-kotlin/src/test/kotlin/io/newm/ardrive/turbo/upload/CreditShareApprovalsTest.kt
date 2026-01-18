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
import io.newm.ardrive.turbo.upload.util.TestJwkFactory
import io.newm.ardrive.turbo.util.HttpClientFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class CreditShareApprovalsTest {
    @Test
    fun `listCreditShares returns approvals`() =
        runBlocking {
            val engine = MockEngine { request ->
                assertThat(request.method).isEqualTo(HttpMethod.Get)
                assertThat(request.url.encodedPath).isEqualTo("/v1/account/approvals/all")
                respond(
                    content =
                        """
                        {"givenApprovals":[],"receivedApprovals":[{"approvalDataItemId":"id","approvedAddress":"addr","payingAddress":"payer","approvedWincAmount":"1","usedWincAmount":"0","creationDate":"now"}]}
                        """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = io.ktor.http.headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
            val config = TurboConfig(
                uploadBaseUrl = "https://upload.ardrive.io",
                httpClient = HttpClient(engine) {
                    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                },
            )
            val client = HttpClientFactory.create(config)
            val service = UploadServiceImpl(
                config = config,
                signer = ArweaveSigner(TestJwkFactory.create()),
                httpClient = client,
            )

            val approvals = service.listCreditShares("addr")

            assertThat(approvals.receivedApprovals).hasSize(1)
        }

    @Test
    fun `getCreditApprovals returns empty on 404`() =
        runBlocking {
            val engine = MockEngine { request ->
                assertThat(request.method).isEqualTo(HttpMethod.Get)
                assertThat(request.url.encodedPath).isEqualTo("/v1/account/approvals/get")
                respond(
                    content = "Not Found",
                    status = HttpStatusCode.NotFound,
                    headers = io.ktor.http.headersOf(HttpHeaders.ContentType, "text/plain"),
                )
            }
            val config = TurboConfig(
                uploadBaseUrl = "https://upload.ardrive.io",
                httpClient = HttpClient(engine) {
                    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                },
            )
            val client = HttpClientFactory.create(config)
            val service = UploadServiceImpl(
                config = config,
                signer = ArweaveSigner(TestJwkFactory.create()),
                httpClient = client,
            )

            val approvals = service.getCreditApprovals("addr")

            assertThat(approvals.givenApprovals).isEmpty()
            assertThat(approvals.receivedApprovals).isEmpty()
        }
}
