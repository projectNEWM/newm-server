package io.projectnewm.server.features.song

import com.google.common.truth.Truth.assertThat
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.projectnewm.server.BaseApplicationTests
import io.projectnewm.server.ext.exists
import io.projectnewm.server.features.song.database.SongEntity
import io.projectnewm.server.features.song.database.SongTable
import io.projectnewm.server.features.song.model.Song
import io.projectnewm.server.features.song.model.SongIdBody
import io.projectnewm.server.features.user.database.UserTable
import java.time.LocalDateTime
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SongRoutesTests : BaseApplicationTests() {

    @BeforeEach
    fun beforeEach() {
        transaction {
            SongTable.deleteAll()
        }
    }

    @Test
    fun testPostSong() = runBlocking {
        val startTime = LocalDateTime.now()

        // Post
        val response = client.post("v1/songs") {
            bearerAuth(testUserToken)
            contentType(ContentType.Application.Json)
            setBody(testSong1)
        }
        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        val songId = response.body<SongIdBody>().songId

        // Read Song directly from database & verify it
        val song = transaction { SongEntity[songId] }.toModel()
        assertThat(song.id).isEqualTo(songId)
        assertThat(song.ownerId).isEqualTo(testUserId)
        assertThat(song.createdAt).isAtLeast(startTime)
        assertThat(song.title).isEqualTo(testSong1.title)
        assertThat(song.genre).isEqualTo(testSong1.genre)
        assertThat(song.description).isEqualTo(testSong1.description)
        assertThat(song.credits).isEqualTo(testSong1.credits)
    }

    @Test
    fun testGetSong() = runBlocking {
        // Add Song directly into database
        val song = transaction {
            SongEntity.new {
                ownerId = EntityID(testUserId, UserTable)
                title = testSong1.title!!
                genre = testSong1.genre
                covertArtUrl = testSong1.covertArtUrl
                description = testSong1.description
                credits = testSong1.credits
            }
        }.toModel()

        // Get it
        val response = client.get("v1/songs/${song.id}") {
            bearerAuth(testUserToken)
            accept(ContentType.Application.Json)
        }
        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        assertThat(response.body<Song>()).isEqualTo(song)
    }

    @Test
    fun testGetSongs() = runBlocking {
        // Add Songs directly into database
        val songs = mutableListOf<Song>()
        for (song in testSongs) {
            songs += transaction {
                SongEntity.new {
                    ownerId = EntityID(testUserId, UserTable)
                    title = song.title!!
                    genre = song.genre
                    covertArtUrl = song.covertArtUrl
                    description = song.description
                    credits = song.credits
                }
            }.toModel()
        }

        // Get all songs & verify
        val response = client.get("v1/songs") {
            bearerAuth(testUserToken)
            accept(ContentType.Application.Json)
        }
        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        assertThat(response.body<List<Song>>()).isEqualTo(songs)
    }

    @Test
    fun testPatchSong() = runBlocking {
        // Add Song1 directly into database
        val songId = transaction {
            SongEntity.new {
                ownerId = EntityID(testUserId, UserTable)
                title = testSong1.title!!
                genre = testSong1.genre
                covertArtUrl = testSong1.covertArtUrl
                description = testSong1.description
                credits = testSong1.credits
            }
        }.id.value

        // Patch it with Song2
        val response = client.patch("v1/songs/$songId") {
            bearerAuth(testUserToken)
            contentType(ContentType.Application.Json)
            setBody(testSong2)
        }
        assertThat(response.status).isEqualTo(HttpStatusCode.NoContent)

        // Read Song directly from database & verify it
        val song = transaction { SongEntity[songId] }.toModel()
        assertThat(song.id).isEqualTo(songId)
        assertThat(song.ownerId).isEqualTo(testUserId)
        assertThat(song.title).isEqualTo(testSong2.title)
        assertThat(song.genre).isEqualTo(testSong2.genre)
        assertThat(song.description).isEqualTo(testSong2.description)
        assertThat(song.credits).isEqualTo(testSong2.credits)
    }

    @Test
    fun testDeleteSong() = runBlocking {
        // Add song directly into database
        val songId = transaction {
            SongEntity.new {
                ownerId = EntityID(testUserId, UserTable)
                title = testSong1.title!!
                genre = testSong1.genre
                covertArtUrl = testSong1.covertArtUrl
                description = testSong1.description
                credits = testSong1.credits
            }
        }.id.value

        // delete it
        val response = client.delete("v1/songs/$songId") {
            bearerAuth(testUserToken)
        }
        assertThat(response.status).isEqualTo(HttpStatusCode.NoContent)

        // make sure doesn't exist in database
        val exists = transaction { SongEntity.exists(songId) }
        assertThat(exists).isFalse()
    }
}
