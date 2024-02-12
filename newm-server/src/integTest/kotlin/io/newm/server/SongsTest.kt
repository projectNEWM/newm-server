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
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

const val SONGS_PAGE_LIMIT = 25

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SongsTest {
    @BeforeAll
    fun beforeAllTests() {
        runBlocking {
            // purge data from prior test runs if requested via newm.integTest.clearTestData property
            var offset = 0

            val clearTestDataOption = (System.getProperty("newm.integTest.clearTestData") ?: "false").toBoolean()
            if (clearTestDataOption) {
                println("System property 'newm.integTest.clearTestData' is true, cleaning test data")
                while (true) {
                    val getSongsResponse =
                        TestContext.client.get("${TestContext.baseUrl}/v1/songs?ownerIds=me&offset=$offset&limit=$SONGS_PAGE_LIMIT") {
                            bearerAuth(TestContext.loginResponse.accessToken)
                        }
                    val songs = getSongsResponse.body<List<Song>>()
                    if (songs.isEmpty()) {
                        break
                    }
                    songs.forEach { song ->
                        print("Deleting song: ${song.id} ${song.title?.take(20)}...")
                        val deleteSongResponse =
                            TestContext.client.delete("${TestContext.baseUrl}/v1/songs/${song.id}") {
                                bearerAuth(TestContext.loginResponse.accessToken)
                            }
                        println(deleteSongResponse.status)
                    }
                    offset += SONGS_PAGE_LIMIT
                }
            }
        }
    }

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
}
