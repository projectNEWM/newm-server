package io.newm.server

import com.google.common.truth.Truth.assertThat
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.newm.server.features.model.CountResponse
import io.newm.server.features.song.model.Song
import io.newm.server.features.song.model.SongIdBody
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class SongsTest {
    @Test
    fun `Add a new song`(): Unit =
        runBlocking {
            val createSongResponse =
                createSong(
                    Song(
                        title = "[TEST] ${String.randomString(8)}",
                        genres = listOf("Synthwave", "Synthpop")
                    )
                )
            assertThat(createSongResponse.status).isEqualTo(HttpStatusCode.OK)
            val resp = createSongResponse.body<SongIdBody>()
            assertThat(resp.songId).isNotNull()

            cleanUpSong(createSongResponse)
        }

    @Test
    fun `Omitting title when adding a new song returns 400`(): Unit =
        runBlocking {
            val createSongResponse =
                createSong(
                    Song(
                        genres = listOf("Synthwave", "Synthpop")
                    )
                )
            assertThat(createSongResponse.status).isEqualTo(HttpStatusCode.UnprocessableEntity)

            cleanUpSong(createSongResponse)
        }

    @Test
    fun `Get an existing song`(): Unit =
        runBlocking {
            val title = "[TEST] ${String.randomString(8)}"
            val createSongResponse =
                createSong(
                    Song(
                        title = title,
                        genres = listOf("Synthwave", "Synthpop")
                    )
                )
            assertThat(createSongResponse.status).isEqualTo(HttpStatusCode.OK)
            val resp = createSongResponse.body<SongIdBody>()
            assertThat(resp.songId).isNotNull()

            val getSongResponse =
                TestContext.client.get("${TestContext.baseUrl}/v1/songs/${resp.songId}") {
                    bearerAuth(TestContext.loginResponse.accessToken)
                }
            assertThat(getSongResponse.status).isEqualTo(HttpStatusCode.OK)
            val song = getSongResponse.body<Song>()
            assertThat(song.title).isEqualTo(title)
            assertThat(song.genres).containsExactly("Synthwave", "Synthpop")

            cleanUpSong(createSongResponse)
        }

    @Test
    fun `Get song count`(): Unit =
        runBlocking {
            val getSongResponse =
                TestContext.client.get("${TestContext.baseUrl}/v1/songs/count") {
                    bearerAuth(TestContext.loginResponse.accessToken)
                }
            assertThat(getSongResponse.status).isEqualTo(HttpStatusCode.OK)
            val resp = getSongResponse.body<CountResponse>()
            assertThat(resp.count).isAtLeast(0L)
        }

    private suspend fun createSong(song: Song): HttpResponse {
        return TestContext.client.post("${TestContext.baseUrl}/v1/songs") {
            bearerAuth(TestContext.loginResponse.accessToken)
            contentType(ContentType.Application.Json)
            setBody(song)
        }
    }

    private suspend fun deleteSong(songId: String): HttpResponse {
        return TestContext.client.delete("${TestContext.baseUrl}/v1/songs/$songId") {
            bearerAuth(TestContext.loginResponse.accessToken)
            contentType(ContentType.Application.Json)
        }
    }

    private suspend fun cleanUpSong(songId: String) {
        try {
            deleteSong(songId)
        } catch (_: Exception) {
        }
    }

    private suspend fun cleanUpSong(httpResponse: HttpResponse) = cleanUpSong(httpResponse.body<SongIdBody>().songId.toString())
}
