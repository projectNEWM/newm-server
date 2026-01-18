package io.newm.ardrive.turbo.upload.chunked

import com.google.common.truth.Truth.assertThat
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.newm.ardrive.turbo.TurboConfig
import io.newm.ardrive.turbo.upload.model.ChunkingMode
import io.newm.ardrive.turbo.upload.model.MultiPartStatus
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class ChunkedUploaderTest {
    @Test
    fun `shouldChunkUpload respects mode`() {
        val uploader = ChunkedUploader(TurboConfig(), HttpClient(MockEngine { respond("{}") }))
        val dataSize = ChunkedUploadConstants.DEFAULT_CHUNK_BYTE_COUNT * 3

        assertThat(uploader.shouldChunkUpload(ChunkingMode.AUTO, ChunkedUploadConstants.DEFAULT_CHUNK_BYTE_COUNT, dataSize)).isTrue()
        assertThat(uploader.shouldChunkUpload(ChunkingMode.DISABLED, ChunkedUploadConstants.DEFAULT_CHUNK_BYTE_COUNT, dataSize)).isFalse()
        assertThat(uploader.shouldChunkUpload(ChunkingMode.FORCE, ChunkedUploadConstants.DEFAULT_CHUNK_BYTE_COUNT, dataSize)).isTrue()
    }

    @Test
    fun `upload hits init and status endpoints`() =
        runBlocking {
            val calls = mutableListOf<String>()
            val engine = MockEngine { request ->
                calls.add("${request.method.value} ${request.url.encodedPath}")
                when {
                    request.url.encodedPath.endsWith("/status") -> respond(
                        content = """{"status":"FINALIZED","receipt":{"id":"id","deadlineHeight":1,"timestamp":1,"version":"1","dataCaches":[],"fastFinalityIndexes":[],"winc":"1","owner":"owner","public":"pub","signature":"sig"}}""",
                        status = HttpStatusCode.OK,
                        headers = io.ktor.http.headersOf(HttpHeaders.ContentType, "application/json"),
                    )

                    request.url.encodedPath.endsWith("/finalize") -> respond(
                        content = "{}",
                        status = HttpStatusCode.OK,
                        headers = io.ktor.http.headersOf(HttpHeaders.ContentType, "application/json"),
                    )

                    request.url.encodedPath.contains("/chunks/") && request.method == HttpMethod.Get -> respond(
                        content = """{"id":"upload","min":1,"max":1,"chunkSize":${ChunkedUploadConstants.DEFAULT_CHUNK_BYTE_COUNT}}""",
                        status = HttpStatusCode.OK,
                        headers = io.ktor.http.headersOf(HttpHeaders.ContentType, "application/json"),
                    )

                    else -> respond("{}", HttpStatusCode.OK)
                }
            }
            val client = HttpClient(engine) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            }
            val uploader = ChunkedUploader(TurboConfig(uploadBaseUrl = "https://upload.ardrive.io"), client)

            uploader.upload(
                token = "arweave",
                dataItem = ByteArray(ChunkedUploadConstants.DEFAULT_CHUNK_BYTE_COUNT.toInt() * 3),
                dataItemOptions = null,
            )

            assertThat(calls.any { it.contains("/v1/chunks/arweave/-1/-1") }).isTrue()
            assertThat(calls.any { it.contains("/v1/chunks/arweave/upload/finalize") }).isTrue()
            assertThat(calls.any { it.contains("/v1/chunks/arweave/upload/status") }).isTrue()
        }
}
