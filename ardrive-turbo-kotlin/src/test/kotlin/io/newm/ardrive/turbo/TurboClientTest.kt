package io.newm.ardrive.turbo

import com.google.common.truth.Truth.assertThat
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.newm.ardrive.turbo.auth.ArweaveSigner
import io.newm.ardrive.turbo.upload.model.UploadEvents
import io.newm.ardrive.turbo.upload.model.UploadFileDescriptor
import io.newm.ardrive.turbo.upload.model.UploadStep
import io.newm.ardrive.turbo.upload.util.TestJwkFactory
import java.io.ByteArrayInputStream
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class TurboClientTest {
    @Test
    fun `authenticated client wires services`() {
        val signer = ArweaveSigner(TestJwkFactory.create())
        val client = TurboClientImpl(TurboConfig(), signer)

        assertThat(client.uploadService).isNotNull()
        assertThat(client.paymentService).isNotNull()
        assertThat(client.getWalletAddress()).isNotEmpty()
    }

    @Test
    fun `upload convenience method delegates`() =
        runBlocking {
            val engine = MockEngine {
                respond(
                    content = """{"dataCaches":[],"fastFinalityIndexes":[],"id":"id","owner":"owner","winc":"1"}""",
                    status = HttpStatusCode.OK,
                    headers = io.ktor.http.headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
            val httpClient = HttpClient(engine) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            }
            val signer = ArweaveSigner(TestJwkFactory.create())
            val client = TurboClientImpl(TurboConfig(httpClient = httpClient), signer, httpClient = httpClient)

            val response = client.uploadDataItem("data".toByteArray(), contentType = "text/plain")

            assertThat(response.id).isEqualTo("id")
        }

    @Test
    fun `uploadFolder builds manifest`() =
        runBlocking {
            val engine = MockEngine {
                respond(
                    content = """{"dataCaches":[],"fastFinalityIndexes":[],"id":"file","owner":"owner","winc":"1"}""",
                    status = HttpStatusCode.OK,
                    headers = io.ktor.http.headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
            val httpClient = HttpClient(engine) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            }
            val signer = ArweaveSigner(TestJwkFactory.create())
            val client = TurboClientImpl(TurboConfig(httpClient = httpClient), signer, httpClient = httpClient)
            var lastStep: UploadStep? = null

            val response = client.uploadFolder(
                files = listOf(
                    UploadFileDescriptor(
                        path = "index.html",
                        contentType = "text/html",
                        streamFactory = { ByteArrayInputStream("data".toByteArray()) },
                        sizeFactory = { 4 },
                    ),
                ),
                events = UploadEvents(onProgress = { lastStep = it.step }),
            )

            assertThat(response.manifest).isNotNull()
            assertThat(response.manifest?.index?.path).isEqualTo("index.html")
            assertThat(lastStep).isNotNull()
        }

    @Test
    fun `unauthenticated client exposes payment service only`() {
        val unauth = TurboClientUnauthenticatedImpl(TurboConfig())

        assertThat(unauth.paymentService).isNotNull()
    }
}
