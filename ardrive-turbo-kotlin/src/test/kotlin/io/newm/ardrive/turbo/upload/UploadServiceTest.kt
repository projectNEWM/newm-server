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
import io.newm.ardrive.turbo.util.HttpClientFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

private fun createSigner(): ArweaveSigner = ArweaveSigner(createTestJwk())

private fun createTestJwk(): String {
    val keyPair = java.security.KeyPairGenerator
        .getInstance("RSA")
        .apply { initialize(4096) }
        .generateKeyPair()
    val privateKey = keyPair.private as java.security.interfaces.RSAPrivateCrtKey
    val jwk = TestJwk(
        kty = "RSA",
        e = privateKey.publicExponent.toBase64Url(),
        n = privateKey.modulus.toBase64Url(),
        d = privateKey.privateExponent.toBase64Url(),
        p = privateKey.primeP.toBase64Url(),
        q = privateKey.primeQ.toBase64Url(),
        dp = privateKey.primeExponentP.toBase64Url(),
        dq = privateKey.primeExponentQ.toBase64Url(),
        qi = privateKey.crtCoefficient.toBase64Url(),
    )
    return Json.encodeToString(jwk)
}

@kotlinx.serialization.Serializable
private data class TestJwk(
    val kty: String,
    val e: String,
    val n: String,
    val d: String,
    val p: String,
    val q: String,
    val dp: String,
    val dq: String,
    val qi: String,
)

private fun java.math.BigInteger.toBase64Url(): String =
    java.util.Base64
        .getUrlEncoder()
        .withoutPadding()
        .encodeToString(toByteArray())

class UploadServiceTest {
    @Test
    fun `uploadDataItem includes signature headers and paidBy`() =
        runBlocking {
            val engine = MockEngine { request ->
                assertThat(request.method).isEqualTo(HttpMethod.Post)
                assertThat(request.headers["x-public-key"]).isNotEmpty()
                assertThat(request.headers["x-nonce"]).isNotEmpty()
                assertThat(request.headers["x-signature"]).isNotEmpty()
                assertThat(request.headers[HttpHeaders.ContentType]).isEqualTo("application/octet-stream")
                assertThat(request.headers["x-paid-by"]).isEqualTo("payer-1,payer-2")
                assertThat(request.headers[HttpHeaders.ContentType]).isEqualTo("application/octet-stream")
                respond(
                    content = """{"dataCaches":[],"fastFinalityIndexes":[],"id":"id","owner":"owner","winc":"1"}""",
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
            val service = UploadServiceImpl(config, createSigner(), client)

            val response = service.uploadDataItem(
                dataItem = "data".toByteArray(),
                dataItemOptions = DataItemOptions(paidBy = listOf("payer-1", "payer-2")),
                token = "arweave",
            )

            assertThat(response.id).isEqualTo("id")
        }

    @Test
    fun `getTransactionStatus uses status endpoint`() =
        runBlocking {
            val engine = MockEngine { request ->
                assertThat(request.url.encodedPath).isEqualTo("/v1/tx/tx-id/status")
                respond(
                    content = """{"status":"FINALIZED","receipt":{"id":"tx-id","deadlineHeight":10,"timestamp":123,"version":"1","dataCaches":[],"fastFinalityIndexes":[],"winc":"1","owner":"owner","public":"pub","signature":"sig"}}""",
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
            val service = UploadServiceImpl(config, createSigner(), client)

            val status = service.getTransactionStatus("tx-id")

            assertThat(status.status).isEqualTo("FINALIZED")
            assertThat(status.receipt?.deadlineHeight).isEqualTo(10)
            assertThat(status.receipt?.publicKey).isEqualTo("pub")
        }

    @Test
    fun `getCreditApprovals returns empty on 404`() =
        runBlocking {
            val engine = MockEngine { request ->
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
            val service = UploadServiceImpl(config, createSigner(), client)

            val approvals = service.getCreditApprovals("addr")

            assertThat(approvals.givenApprovals).isEmpty()
            assertThat(approvals.receivedApprovals).isEmpty()
        }

    @Test
    fun `listCreditShares returns empty on 404`() =
        runBlocking {
            val engine = MockEngine { request ->
                assertThat(request.url.encodedPath).isEqualTo("/v1/account/approvals/all")
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
            val service = UploadServiceImpl(config, createSigner(), client)

            val approvals = service.listCreditShares("addr")

            assertThat(approvals.givenApprovals).isEmpty()
            assertThat(approvals.receivedApprovals).isEmpty()
        }
}
