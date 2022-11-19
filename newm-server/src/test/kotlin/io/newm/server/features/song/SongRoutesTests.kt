package io.newm.server.features.song

import com.amazonaws.HttpMethod
import com.amazonaws.services.s3.AmazonS3
import com.google.common.truth.Truth.assertThat
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.newm.server.BaseApplicationTests
import io.newm.server.di.inject
import io.newm.server.ext.existsHavingId
import io.newm.server.ext.toDate
import io.newm.server.features.song.database.SongEntity
import io.newm.server.features.song.database.SongTable
import io.newm.server.features.song.model.MarketplaceStatus
import io.newm.server.features.song.model.MintingStatus
import io.newm.server.features.song.model.Song
import io.newm.server.features.song.model.SongIdBody
import io.newm.server.features.song.model.UploadRequest
import io.newm.server.features.song.model.UploadResponse
import io.newm.server.features.user.database.UserEntity
import io.newm.server.features.user.database.UserTable
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
        assertThat(song.streamUrl).isEqualTo(testSong1.streamUrl)
        assertThat(song.nftPolicyId).isEqualTo(testSong1.nftPolicyId)
        assertThat(song.nftName).isEqualTo(testSong1.nftName)
        assertThat(song.mintingStatus).isEqualTo(testSong1.mintingStatus)
        assertThat(song.marketplaceStatus).isEqualTo(testSong1.marketplaceStatus)
    }

    @Test
    fun testGetSong() = runBlocking {
        // Add Song directly into database
        val song = transaction {
            SongEntity.new {
                ownerId = EntityID(testUserId, UserTable)
                title = testSong1.title!!
                genre = testSong1.genre!!
                coverArtUrl = testSong1.coverArtUrl
                description = testSong1.description
                credits = testSong1.credits
                streamUrl = testSong1.streamUrl
                nftPolicyId = testSong1.nftPolicyId
                nftName = testSong1.nftName
                mintingStatus = testSong1.mintingStatus
                marketplaceStatus = testSong1.marketplaceStatus
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
    fun testGetAllSongs() = runBlocking {
        // Add Songs directly into database
        val expectedSongs = mutableListOf<Song>()
        for (offset in 0..30) {
            expectedSongs += transaction {
                SongEntity.new {
                    ownerId = EntityID(testUserId, UserTable)
                    title = "title$offset"
                    genre = "genre$offset"
                    coverArtUrl = "coverArtUrl$offset"
                    description = "description$offset"
                    credits = "credits$offset"
                    streamUrl = "streamUrl$offset"
                    nftPolicyId = "nftPolicyId$offset"
                    nftName = "nftName$offset"
                    mintingStatus = MintingStatus.values()[offset % MintingStatus.values().size]
                    marketplaceStatus = MarketplaceStatus.values()[offset % MarketplaceStatus.values().size]
                }
            }.toModel()
        }

        // Get all songs forcing pagination
        var offset = 0
        val limit = 5
        val actualSongs = mutableListOf<Song>()
        while (true) {
            val response = client.get("v1/songs") {
                bearerAuth(testUserToken)
                accept(ContentType.Application.Json)
                parameter("offset", offset)
                parameter("limit", limit)
            }
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val songs = response.body<List<Song>>()
            if (songs.isEmpty()) break
            actualSongs += songs
            offset += limit
        }

        // verify all
        assertThat(actualSongs).isEqualTo(expectedSongs)
    }

    @Test
    fun testPatchSong() = runBlocking {
        // Add Song1 directly into database
        val songId = transaction {
            SongEntity.new {
                ownerId = EntityID(testUserId, UserTable)
                title = testSong1.title!!
                genre = testSong1.genre!!
                coverArtUrl = testSong1.coverArtUrl
                description = testSong1.description
                credits = testSong1.credits
                streamUrl = testSong1.streamUrl
                nftPolicyId = testSong1.nftPolicyId
                nftName = testSong1.nftName
                mintingStatus = testSong1.mintingStatus
                marketplaceStatus = testSong1.marketplaceStatus
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
        assertThat(song.streamUrl).isEqualTo(testSong2.streamUrl)
        assertThat(song.nftPolicyId).isEqualTo(testSong2.nftPolicyId)
        assertThat(song.nftName).isEqualTo(testSong2.nftName)
        assertThat(song.mintingStatus).isEqualTo(testSong2.mintingStatus)
        assertThat(song.marketplaceStatus).isEqualTo(testSong2.marketplaceStatus)
    }

    @Test
    fun testDeleteSong() = runBlocking {
        // Add song directly into database
        val songId = transaction {
            SongEntity.new {
                ownerId = EntityID(testUserId, UserTable)
                title = testSong1.title!!
                genre = testSong1.genre!!
                coverArtUrl = testSong1.coverArtUrl
                description = testSong1.description
                credits = testSong1.credits
                streamUrl = testSong1.streamUrl
                nftPolicyId = testSong1.nftPolicyId
                nftName = testSong1.nftName
                mintingStatus = testSong1.mintingStatus
                marketplaceStatus = testSong1.marketplaceStatus
            }
        }.id.value

        // delete it
        val response = client.delete("v1/songs/$songId") {
            bearerAuth(testUserToken)
        }
        assertThat(response.status).isEqualTo(HttpStatusCode.NoContent)

        // make sure doesn't exist in database
        val exists = transaction { SongEntity.existsHavingId(songId) }
        assertThat(exists).isFalse()
    }

    @Test
    fun testRequestUpload() = runBlocking {
        // Add song directly into database
        val songId = transaction {
            SongEntity.new {
                ownerId = EntityID(testUserId, UserTable)
                title = testSong1.title!!
                genre = testSong1.genre!!
                coverArtUrl = testSong1.coverArtUrl
                description = testSong1.description
                credits = testSong1.credits
                streamUrl = testSong1.streamUrl
                nftPolicyId = testSong1.nftPolicyId
                nftName = testSong1.nftName
                mintingStatus = testSong1.mintingStatus
                marketplaceStatus = testSong1.marketplaceStatus
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

    @Test
    fun testGetAllGenres() = runBlocking {

        val ownerId = transaction {
            UserEntity.new {
                email = "artist1@newm.io"
            }
        }.id.value

        // Add Songs directly into database generating genres histogram
        val expectedGenres = listOf("genreG", "genreF", "genreE", "genreD", "genreC", "genreB", "genreA")
        var frequency = expectedGenres.size
        for (genre in expectedGenres) {
            for (i in 1..frequency) {
                transaction {
                    SongEntity.new {
                        this.ownerId = EntityID(ownerId, UserTable)
                        title = "title_${genre}_$i"
                        this.genre = genre
                    }
                }
            }
            frequency--
        }

        // Get all genres forcing pagination
        var offset = 0
        val limit = 3
        val actualGenres = mutableListOf<String>()
        while (true) {
            val response = client.get("v1/songs/genres") {
                bearerAuth(testUserToken)
                accept(ContentType.Application.Json)
                parameter("offset", offset)
                parameter("limit", limit)
            }
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val genres = response.body<List<String>>()
            if (genres.isEmpty()) break
            actualGenres += genres
            offset += limit
        }

        // verify all
        assertThat(actualGenres).isEqualTo(expectedGenres)
    }

    @Test
    fun testGetGenresByOwner() = runBlocking {

        val ownerId1 = transaction {
            UserEntity.new {
                email = "artist1@newm.io"
            }
        }.id.value

        val ownerId2 = transaction {
            UserEntity.new {
                email = "artist2@newm.io"
            }
        }.id.value

        // Add Songs directly into database generating genres histogram
        val expectedGenres = listOf("genreG", "genreF", "genreE", "genreD", "genreC", "genreB", "genreA")
        var frequency = expectedGenres.size
        for (genre in expectedGenres) {
            // Owner 1 - this is what we will verify
            for (i in 1..frequency) {
                transaction {
                    SongEntity.new {
                        this.ownerId = EntityID(ownerId1, UserTable)
                        title = "title_${genre}_$i"
                        this.genre = genre
                    }
                }
            }
            // Owner 2 - this is what we should filter out
            for (i in (frequency + 1)..expectedGenres.size) {
                transaction {
                    SongEntity.new {
                        this.ownerId = EntityID(ownerId2, UserTable)
                        title = "title_${genre}_$i"
                        this.genre = genre
                    }
                }
            }
            frequency--
        }

        // Get Owner 1 genres forcing pagination
        var offset = 0
        val limit = 3
        val actualGenres = mutableListOf<String>()
        while (true) {
            val response = client.get("v1/songs/genres") {
                bearerAuth(testUserToken)
                accept(ContentType.Application.Json)
                parameter("offset", offset)
                parameter("limit", limit)
                parameter("ownerId", ownerId1)
            }
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val genres = response.body<List<String>>()
            if (genres.isEmpty()) break
            actualGenres += genres
            offset += limit
        }

        // verify all
        assertThat(actualGenres).isEqualTo(expectedGenres)
    }
}
