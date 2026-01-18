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
import io.newm.ardrive.turbo.model.TokenType
import io.newm.ardrive.turbo.upload.util.TestJwkFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class UploadAccountAndPricingTest {
    @Test
    fun `getAccountBalance uses provided account id`() =
        runBlocking {
            val engine = MockEngine { request ->
                assertThat(request.method).isEqualTo(HttpMethod.Get)
                assertThat(request.url.encodedPath).isEqualTo("/v1/account/balance/account-id")
                respond(
                    content = """{"controlledWinc":"1","winc":"2","effectiveBalance":"3","receivedApprovals":[],"givenApprovals":[]}""",
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

            val balance = service.getAccountBalance("account-id")

            assertThat(balance.controlledWinc).isEqualTo("1")
        }

    @Test
    fun `getServiceInfo returns addresses map`() =
        runBlocking {
            val engine = MockEngine { request ->
                assertThat(request.url.encodedPath).isEqualTo("/v1/info")
                respond(
                    content = """{"version":"1","gateway":"https://arweave.net","freeUploadLimitBytes":10,"addresses":{"arweave":"addr"}}""",
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

            val info = service.getServiceInfo()

            assertThat(info.addresses[TokenType.ARWEAVE]).isEqualTo("addr")
        }
}
