package io.newm.server

import com.google.common.truth.Truth.assertThat
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.append
import io.ktor.http.contentType
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.writeFully
import io.newm.server.features.model.CountResponse
import io.newm.server.features.song.model.AudioUploadRequest
import io.newm.server.features.song.model.AudioUploadResponse
import io.newm.server.features.song.model.Song
import io.newm.server.features.song.model.SongIdBody
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import java.io.File
import java.time.Duration
import java.util.concurrent.TimeoutException

class SongsTest {
    @Test
    fun `Add a new song`(): Unit = runBlocking {
        val createSongResponse = createSong(
            Song(
                title = "[TEST] ${String.randomString(8)}",
                genres = listOf("Synthwave", "Synthpop")
            )
        )
        assertThat(createSongResponse.status).isEqualTo(HttpStatusCode.OK)
        val resp = createSongResponse.body<SongIdBody>()
        assertThat(resp.songId)
    }

    @Test
    fun `Omitting title when adding a new song returns 400`(): Unit = runBlocking {
        val createSongResponse = createSong(
            Song(
                genres = listOf("Synthwave", "Synthpop")
            )
        )
        assertThat(createSongResponse.status).isEqualTo(HttpStatusCode.UnprocessableEntity)
    }

    @Test
    fun `Get an existing song`(): Unit = runBlocking {
        val createSongResponse = createSong(
            Song(
                title = "[TEST] ${String.randomString(8)}",
                genres = listOf("Synthwave", "Synthpop")
            )
        )
        assertThat(createSongResponse.status).isEqualTo(HttpStatusCode.OK)
        val resp = createSongResponse.body<SongIdBody>()
        assertThat(resp.songId)

        val getSongResponse = TestContext.client.get("${TestContext.baseUrl}/v1/songs/${resp.songId}") {
            bearerAuth(TestContext.loginResponse.accessToken)
        }
        assertThat(getSongResponse.status).isEqualTo(HttpStatusCode.OK)
    }

    @Test
    fun `Get song count`(): Unit = runBlocking {
        val getSongResponse = TestContext.client.get("${TestContext.baseUrl}/v1/songs/count") {
            bearerAuth(TestContext.loginResponse.accessToken)
        }
        assertThat(getSongResponse.status).isEqualTo(HttpStatusCode.OK)
        val resp = getSongResponse.body<CountResponse>()
        assertThat(resp.count).isAtLeast(0)
    }

    @Test
    fun `Upload audio Happy Path`(): Unit = runBlocking {
        val createSongResponse = createSong(
            Song(
                title = "[TEST] ${String.randomString(8)}",
                genres = listOf("Synthwave", "Synthpop")
            )
        )
        val songId = createSongResponse.body<SongIdBody>().songId
        val fileName = "test1.wav"
        val key = "$songId/$fileName"

        // Request upload
        val response = TestContext.client.post("${TestContext.baseUrl}/v1/songs/$songId/upload") {
            bearerAuth(TestContext.loginResponse.accessToken)
            contentType(ContentType.Application.Json)
            setBody(AudioUploadRequest(fileName))
        }
        assertThat(response.status).isEqualTo(HttpStatusCode.OK)

        with(response.body<AudioUploadResponse>()) {
            assertThat(fields.size).isEqualTo(7)
            assertThat(fields["key"]).isEqualTo(key)
            assertThat(fields["X-Amz-Algorithm"]).isEqualTo("AWS4-HMAC-SHA256")
            assertThat(fields).containsKey("X-Amz-Credential")
            assertThat(fields).containsKey("X-Amz-Date")
            assertThat(fields).containsKey("policy")
            assertThat(fields).containsKey("X-Amz-Signature")
        }

        val audioUploadResponse = response.body<AudioUploadResponse>()
        val file = File(TestContext.config.getString("audio.happyPathFile"))
        val form = formData {
            audioUploadResponse.fields.forEach {
                append(it.key, it.value)
            }
            appendInput(
                key = "file",
                headers = Headers.build {
                    append(HttpHeaders.ContentType, "application/octet-stream")
                    append(HttpHeaders.ContentDisposition, "filename=${file.name}")
                },
                size = file.length()
            ) {
                buildPacket { writeFully(file.readBytes()) }
            }
        }

        val formResp = TestContext.client.submitFormWithBinaryData(form) {
            url(audioUploadResponse.url)
            headers {
                append("Accept", ContentType.Application.Json)
            }
        }

        assertThat(formResp.status.value).isIn(200..204)

        // wait for transcoding to finish
        // poll server until Song is updated
        try {
            withTimeout(Duration.ofMinutes(2).toMillis()) {
                var pollCount = 0
                while (true) {
                    println("Waiting for Song to finish transcoding (attempt: ${++pollCount})")
                    val getSongResponse = TestContext.client.get("${TestContext.baseUrl}/v1/songs/$songId") {
                        bearerAuth(TestContext.loginResponse.accessToken)
                    }
                    if (getSongResponse.status == HttpStatusCode.OK) {
                        val song = getSongResponse.body<Song>()
                        if (!song.streamUrl.isNullOrBlank()) {
                            assertThat(song.streamUrl).isNotNull()
                            break
                        }
                    }
                    delay(5000) // Delay for 5 seconds
                }
            }
        } catch (toe: TimeoutException) {
            throw Exception("Timed out waiting for Song to finish transcoding")
        }
    }

    private suspend fun createSong(song: Song): HttpResponse {
        return TestContext.client.post("${TestContext.baseUrl}/v1/songs") {
            bearerAuth(TestContext.loginResponse.accessToken)
            contentType(ContentType.Application.Json)
            setBody(song)
        }
    }
}
