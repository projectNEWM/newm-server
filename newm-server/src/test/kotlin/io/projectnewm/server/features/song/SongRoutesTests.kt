package io.projectnewm.server.features.song

import com.amazonaws.HttpMethod
import com.amazonaws.services.s3.AmazonS3
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
import io.projectnewm.server.di.inject
import io.projectnewm.server.ext.exists
import io.projectnewm.server.ext.toDate
import io.projectnewm.server.features.song.database.SongEntity
import io.projectnewm.server.features.song.database.SongTable
import io.projectnewm.server.features.song.model.Song
import io.projectnewm.server.features.song.model.SongIdBody
import io.projectnewm.server.features.song.model.UploadRequest
import io.projectnewm.server.features.song.model.UploadResponse
import io.projectnewm.server.features.user.database.UserTable
import java.time.Instant
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
        assertThat(song.audioUrl).isEqualTo(testSong1.audioUrl)
        assertThat(song.nftPolicyId).isEqualTo(testSong1.nftPolicyId)
        assertThat(song.nftName).isEqualTo(testSong1.nftName)
    }

    @Test
    fun testGetSong() = runBlocking {
        // Add Song directly into database
        val song = transaction {
            SongEntity.new {
                ownerId = EntityID(testUserId, UserTable)
                title = testSong1.title!!
                genre = testSong1.genre
                coverArtUrl = testSong1.coverArtUrl
                description = testSong1.description
                credits = testSong1.credits
                audioUrl = testSong1.audioUrl
                nftPolicyId = testSong1.nftPolicyId
                nftName = testSong1.nftName
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
                    coverArtUrl = song.coverArtUrl
                    description = song.description
                    credits = song.credits
                    audioUrl = song.audioUrl
                    nftPolicyId = song.nftPolicyId
                    nftName = song.nftName
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
                coverArtUrl = testSong1.coverArtUrl
                description = testSong1.description
                credits = testSong1.credits
                audioUrl = testSong1.audioUrl
                nftPolicyId = testSong1.nftPolicyId
                nftName = testSong1.nftName
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
        assertThat(song.audioUrl).isEqualTo(testSong2.audioUrl)
        assertThat(song.nftPolicyId).isEqualTo(testSong2.nftPolicyId)
        assertThat(song.nftName).isEqualTo(testSong2.nftName)
    }

    @Test
    fun testDeleteSong() = runBlocking {
        // Add song directly into database
        val songId = transaction {
            SongEntity.new {
                ownerId = EntityID(testUserId, UserTable)
                title = testSong1.title!!
                genre = testSong1.genre
                coverArtUrl = testSong1.coverArtUrl
                description = testSong1.description
                credits = testSong1.credits
                audioUrl = testSong1.audioUrl
                nftPolicyId = testSong1.nftPolicyId
                nftName = testSong1.nftName
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

    @Test
    fun testRequestUpload() = runBlocking {
        // Add song directly into database
        val songId = transaction {
            SongEntity.new {
                ownerId = EntityID(testUserId, UserTable)
                title = testSong1.title!!
                genre = testSong1.genre
                coverArtUrl = testSong1.coverArtUrl
                description = testSong1.description
                credits = testSong1.credits
                audioUrl = testSong1.audioUrl
                nftPolicyId = testSong1.nftPolicyId
                nftName = testSong1.nftName
            }
        }.id.value

        val start = Instant.now()

        // Request upload
        val response = client.post("v1/songs/$songId/upload") {
            bearerAuth(testUserToken)
            contentType(ContentType.Application.Json)
            setBody(UploadRequest("test1.mp3"))
        }
        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        val resp = response.body<UploadResponse>()

        // allow for some variation on ttl
        val s3 by inject<AmazonS3>()
        val validUrls = mutableListOf<String>()
        for (ttl in 180L..182L) {
            validUrls += s3.generatePresignedUrl(
                "newm-test",
                "$songId/test1.mp3",
                start.plusSeconds(ttl).toDate(),
                HttpMethod.PUT
            ).toString()
        }
        assertThat(resp.uploadUrl).isIn(validUrls)
    }
}
