package io.newm.server.features.song

import com.amazonaws.HttpMethod
import com.amazonaws.services.s3.AmazonS3
import com.google.common.truth.Truth.assertThat
import com.google.iot.cbor.CborInteger
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.utils.io.core.*
import io.newm.chain.util.toHexString
import io.newm.server.BaseApplicationTests
import io.newm.server.features.cardano.database.KeyEntity
import io.newm.server.features.cardano.database.KeyTable
import io.newm.server.features.model.CountResponse
import io.newm.server.features.song.database.SongEntity
import io.newm.server.features.song.database.SongTable
import io.newm.server.features.song.model.*
import io.newm.server.features.user.database.UserEntity
import io.newm.server.features.user.database.UserTable
import io.newm.shared.ext.*
import io.newm.shared.koin.inject
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.EOFException
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

class SongRoutesTests : BaseApplicationTests() {

    @BeforeEach
    fun beforeEach() {
        transaction {
            SongTable.deleteAll()
            KeyTable.deleteAll()
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
        assertThat(song.genres).isEqualTo(testSong1.genres)
        assertThat(song.moods).isEqualTo(testSong1.moods)
        assertThat(song.description).isEqualTo(testSong1.description)
        assertThat(song.credits).isEqualTo(testSong1.credits)
        assertThat(song.duration).isEqualTo(testSong1.duration)
        assertThat(song.streamUrl).isEqualTo(testSong1.streamUrl)
        assertThat(song.nftPolicyId).isEqualTo(testSong1.nftPolicyId)
        assertThat(song.nftName).isEqualTo(testSong1.nftName)
        assertThat(song.mintingStatus).isEqualTo(testSong1.mintingStatus)
        assertThat(song.marketplaceStatus).isEqualTo(testSong1.marketplaceStatus)
    }

    @Test
    fun testGetSong() = runBlocking {
        transaction {
            KeyEntity.new(id = UUID.fromString("00000000-0000-0000-0000-000000000000")) {
                this.address = testKey.address
                this.vkey = testKey.vkey.toHexString()
                this.skey = testKey.skey.toHexString()
            }
        }

        // Add Song directly into database
        val song = transaction {
            SongEntity.new {
                ownerId = EntityID(testUserId, UserTable)
                title = testSong1.title!!
                genres = testSong1.genres!!.toTypedArray()
                moods = testSong1.moods!!.toTypedArray()
                coverArtUrl = testSong1.coverArtUrl
                description = testSong1.description
                credits = testSong1.credits
                duration = testSong1.duration
                streamUrl = testSong1.streamUrl
                nftPolicyId = testSong1.nftPolicyId
                nftName = testSong1.nftName
                mintingStatus = testSong1.mintingStatus!!
                marketplaceStatus = testSong1.marketplaceStatus!!
                paymentKeyId = EntityID(testSong1.paymentKeyId!!, KeyTable)
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
        // Add Users + Songs directly into database
        val expectedSongs = mutableListOf<Song>()
        for (offset in 0..30) {
            val ownerId = transaction {
                UserEntity.new {
                    email = "artist$offset@newm.io"
                }
            }.id.value

            expectedSongs += transaction {
                SongEntity.new {
                    this.ownerId = EntityID(ownerId, UserTable)
                    title = "title$offset"
                    genres = arrayOf("genre${offset}_0", "genre${offset}_1")
                    moods = arrayOf("mood${offset}_0", "mood${offset}_1")
                    coverArtUrl = "coverArtUrl$offset"
                    description = "description$offset"
                    credits = "credits$offset"
                    duration = offset
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
    fun testGetSongsByIds() = runBlocking {
        // Add Users + Songs directly into database
        val allSongs = mutableListOf<Song>()
        for (offset in 0..30) {
            val ownerId = transaction {
                UserEntity.new {
                    email = "artist$offset@newm.io"
                }
            }.id.value

            allSongs += transaction {
                SongEntity.new {
                    this.ownerId = EntityID(ownerId, UserTable)
                    title = "title$offset"
                    genres = arrayOf("genre${offset}_0", "genre${offset}_1")
                    moods = arrayOf("mood${offset}_0", "mood${offset}_1")
                    coverArtUrl = "coverArtUrl$offset"
                    description = "description$offset"
                    credits = "credits$offset"
                    duration = offset
                    streamUrl = "streamUrl$offset"
                    nftPolicyId = "nftPolicyId$offset"
                    nftName = "nftName$offset"
                    mintingStatus = MintingStatus.values()[offset % MintingStatus.values().size]
                    marketplaceStatus = MarketplaceStatus.values()[offset % MarketplaceStatus.values().size]
                }
            }.toModel()
        }

        // filter out 1st and last
        val expectedSongs = allSongs.subList(1, allSongs.size - 1)
        val ids = expectedSongs.map { it.id }.joinToString()

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
                parameter("ids", ids)
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
    fun testGetSongsByOwnerIds() = runBlocking {
        // Add Users + Songs directly into database
        val allSongs = mutableListOf<Song>()
        for (offset in 0..30) {
            val ownerId = transaction {
                UserEntity.new {
                    email = "artist$offset@newm.io"
                }
            }.id.value

            allSongs += transaction {
                SongEntity.new {
                    this.ownerId = EntityID(ownerId, UserTable)
                    title = "title$offset"
                    genres = arrayOf("genre${offset}_0", "genre${offset}_1")
                    moods = arrayOf("mood${offset}_0", "mood${offset}_1")
                    coverArtUrl = "coverArtUrl$offset"
                    description = "description$offset"
                    credits = "credits$offset"
                    duration = offset
                    streamUrl = "streamUrl$offset"
                    nftPolicyId = "nftPolicyId$offset"
                    nftName = "nftName$offset"
                    mintingStatus = MintingStatus.values()[offset % MintingStatus.values().size]
                    marketplaceStatus = MarketplaceStatus.values()[offset % MarketplaceStatus.values().size]
                }
            }.toModel()
        }

        // filter out 1st and last
        val expectedSongs = allSongs.subList(1, allSongs.size - 1)
        val ownerIds = expectedSongs.map { it.ownerId }.joinToString()

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
                parameter("ownerIds", ownerIds)
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
    fun testGetSongsByGenres() = runBlocking {
        // Add Users + Songs directly into database
        val allSongs = mutableListOf<Song>()
        for (offset in 0..30) {
            val ownerId = transaction {
                UserEntity.new {
                    email = "artist$offset@newm.io"
                }
            }.id.value

            allSongs += transaction {
                SongEntity.new {
                    this.ownerId = EntityID(ownerId, UserTable)
                    title = "title$offset"
                    genres = arrayOf("genre${offset}_0", "genre${offset}_1")
                    moods = arrayOf("mood${offset}_0", "mood${offset}_1")
                    coverArtUrl = "coverArtUrl$offset"
                    description = "description$offset"
                    credits = "credits$offset"
                    duration = offset
                    streamUrl = "streamUrl$offset"
                    nftPolicyId = "nftPolicyId$offset"
                    nftName = "nftName$offset"
                    mintingStatus = MintingStatus.values()[offset % MintingStatus.values().size]
                    marketplaceStatus = MarketplaceStatus.values()[offset % MarketplaceStatus.values().size]
                }
            }.toModel()
        }

        // filter out 1st and last and take only 1st genre of each
        val expectedSongs = allSongs.subList(1, allSongs.size - 1)
        val genres = expectedSongs.joinToString { it.genres!!.first() }

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
                parameter("genres", genres)
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
    fun testGetSongsByMoods() = runBlocking {
        // Add Users + Songs directly into database
        val allSongs = mutableListOf<Song>()
        for (offset in 0..30) {
            val ownerId = transaction {
                UserEntity.new {
                    email = "artist$offset@newm.io"
                }
            }.id.value

            allSongs += transaction {
                SongEntity.new {
                    this.ownerId = EntityID(ownerId, UserTable)
                    title = "title$offset"
                    genres = arrayOf("genre${offset}_0", "genre${offset}_1")
                    moods = arrayOf("mood${offset}_0", "mood${offset}_1")
                    coverArtUrl = "coverArtUrl$offset"
                    description = "description$offset"
                    credits = "credits$offset"
                    duration = offset
                    streamUrl = "streamUrl$offset"
                    nftPolicyId = "nftPolicyId$offset"
                    nftName = "nftName$offset"
                    mintingStatus = MintingStatus.values()[offset % MintingStatus.values().size]
                    marketplaceStatus = MarketplaceStatus.values()[offset % MarketplaceStatus.values().size]
                }
            }.toModel()
        }

        // filter out 1st and last and take only 1st mood of each
        val expectedSongs = allSongs.subList(1, allSongs.size - 1)
        val moods = expectedSongs.joinToString { it.moods!!.first() }

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
                parameter("moods", moods)
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
    fun testGetSongsByOlderThan() = runBlocking {
        // Add Users + Songs directly into database
        val allSongs = mutableListOf<Song>()
        for (offset in 0..30) {
            val ownerId = transaction {
                UserEntity.new {
                    email = "artist$offset@newm.io"
                }
            }.id.value

            allSongs += transaction {
                SongEntity.new {
                    this.ownerId = EntityID(ownerId, UserTable)
                    title = "title$offset"
                    genres = arrayOf("genre${offset}_0", "genre${offset}_1")
                    moods = arrayOf("mood${offset}_0", "mood${offset}_1")
                    coverArtUrl = "coverArtUrl$offset"
                    description = "description$offset"
                    credits = "credits$offset"
                    duration = offset
                    streamUrl = "streamUrl$offset"
                    nftPolicyId = "nftPolicyId$offset"
                    nftName = "nftName$offset"
                    mintingStatus = MintingStatus.values()[offset % MintingStatus.values().size]
                    marketplaceStatus = MarketplaceStatus.values()[offset % MarketplaceStatus.values().size]
                }
            }.toModel()
        }

        // filter out newest one
        val expectedSongs = allSongs.subList(0, allSongs.size - 1)
        val olderThan = allSongs.last().createdAt

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
                parameter("olderThan", olderThan)
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
    fun testGetSongsByNewerThan() = runBlocking {
        // Add Users + Songs directly into database
        val allSongs = mutableListOf<Song>()
        for (offset in 0..30) {
            val ownerId = transaction {
                UserEntity.new {
                    email = "artist$offset@newm.io"
                }
            }.id.value

            allSongs += transaction {
                SongEntity.new {
                    this.ownerId = EntityID(ownerId, UserTable)
                    title = "title$offset"
                    genres = arrayOf("genre${offset}_0", "genre${offset}_1")
                    moods = arrayOf("mood${offset}_0", "mood${offset}_1")
                    coverArtUrl = "coverArtUrl$offset"
                    description = "description$offset"
                    credits = "credits$offset"
                    duration = offset
                    streamUrl = "streamUrl$offset"
                    nftPolicyId = "nftPolicyId$offset"
                    nftName = "nftName$offset"
                    mintingStatus = MintingStatus.values()[offset % MintingStatus.values().size]
                    marketplaceStatus = MarketplaceStatus.values()[offset % MarketplaceStatus.values().size]
                }
            }.toModel()
        }

        // filter out oldest one
        val expectedSongs = allSongs.subList(1, allSongs.size)
        val newerThan = allSongs.first().createdAt

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
                parameter("newerThan", newerThan)
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
    fun testGetSongsByPhrase() = runBlocking {
        val phrase = "ABCDE"
        fun phraseOrBlank(offset: Int, target: Int) = if (offset % 4 == target) phrase else ""

        // Add Users + Songs directly into database
        val allSongs = mutableListOf<Song>()
        for (offset in 0..30) {
            val ownerId = transaction {
                UserEntity.new {
                    email = "artist$offset@newm.io"
                }
            }.id.value

            allSongs += transaction {
                SongEntity.new {
                    this.ownerId = EntityID(ownerId, UserTable)
                    title = "title$offset ${phraseOrBlank(offset, 0)} blah blah"
                    genres = arrayOf("genre${offset}_0", "genre${offset}_1")
                    moods = arrayOf("mood${offset}_0", "mood${offset}_1")
                    coverArtUrl = "coverArtUrl$offset"
                    description = "description$offset ${phraseOrBlank(offset, 1)} blah blah"
                    credits = "credits$offset ${phraseOrBlank(offset, 2)} blah blah"
                    duration = offset
                    streamUrl = "streamUrl$offset"
                    nftPolicyId = "nftPolicyId$offset"
                    nftName = "nftName$offset ${phraseOrBlank(offset, 3)} blah blah"
                    mintingStatus = MintingStatus.values()[offset % MintingStatus.values().size]
                    marketplaceStatus = MarketplaceStatus.values()[offset % MarketplaceStatus.values().size]
                }
            }.toModel()
        }

        // filter out for phrase
        val expectedSongs = allSongs.filter {
            phrase in it.title!! || phrase in it.description!! || phrase in it.credits!! || phrase in it.nftName!!
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
                parameter("phrase", phrase)
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
                genres = testSong1.genres!!.toTypedArray()
                moods = testSong1.moods!!.toTypedArray()
                coverArtUrl = testSong1.coverArtUrl
                description = testSong1.description
                credits = testSong1.credits
                duration = testSong1.duration
                streamUrl = testSong1.streamUrl
                nftPolicyId = testSong1.nftPolicyId
                nftName = testSong1.nftName
                mintingStatus = testSong1.mintingStatus!!
                marketplaceStatus = testSong1.marketplaceStatus!!
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
        assertThat(song.genres).isEqualTo(testSong2.genres)
        assertThat(song.moods).isEqualTo(testSong2.moods)
        assertThat(song.description).isEqualTo(testSong2.description)
        assertThat(song.credits).isEqualTo(testSong2.credits)
        assertThat(song.duration).isEqualTo(testSong2.duration)
        assertThat(song.streamUrl).isEqualTo(testSong2.streamUrl)
        assertThat(song.nftPolicyId).isEqualTo(testSong2.nftPolicyId)
        assertThat(song.nftName).isEqualTo(testSong2.nftName)
        assertThat(song.mintingStatus).isEqualTo(testSong1.mintingStatus) // no change expected
        assertThat(song.marketplaceStatus).isEqualTo(testSong1.marketplaceStatus) // no change expected
    }

    @Test
    fun testDeleteSong() = runBlocking {
        // Add song directly into database
        val songId = transaction {
            SongEntity.new {
                ownerId = EntityID(testUserId, UserTable)
                title = testSong1.title!!
                genres = testSong1.genres!!.toTypedArray()
                moods = testSong1.moods!!.toTypedArray()
                coverArtUrl = testSong1.coverArtUrl
                description = testSong1.description
                credits = testSong1.credits
                duration = testSong1.duration
                streamUrl = testSong1.streamUrl
                nftPolicyId = testSong1.nftPolicyId
                nftName = testSong1.nftName
                mintingStatus = testSong1.mintingStatus!!
                marketplaceStatus = testSong1.marketplaceStatus!!
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
    fun testRequestAudioUpload() = runBlocking {
        // Add song directly into database
        val songId = transaction {
            SongEntity.new {
                ownerId = EntityID(testUserId, UserTable)
                title = testSong1.title!!
                genres = testSong1.genres!!.toTypedArray()
                moods = testSong1.moods!!.toTypedArray()
                coverArtUrl = testSong1.coverArtUrl
                description = testSong1.description
                credits = testSong1.credits
                duration = testSong1.duration
                streamUrl = testSong1.streamUrl
                nftPolicyId = testSong1.nftPolicyId
                nftName = testSong1.nftName
                mintingStatus = testSong1.mintingStatus!!
                marketplaceStatus = testSong1.marketplaceStatus!!
            }
        }.id.value

        val start = Instant.now()

        // Request upload
        val response = client.post("v1/songs/$songId/audio") {
            bearerAuth(testUserToken)
            contentType(ContentType.Application.Json)
            setBody(UploadAudioRequest("test1.mp3"))
        }
        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        val resp = response.body<UploadAudioResponse>()

        // allow for some variation on ttl
        val s3 by inject<AmazonS3>()
        val validUrls = mutableListOf<String>()
        for (ttl in 180L..182L) {
            validUrls += s3.generatePresignedUrl(
                "test-audio",
                "$songId/test1.mp3",
                start.plusSeconds(ttl).toDate(),
                HttpMethod.PUT
            ).toString()
        }
        assertThat(resp.uploadUrl).isIn(validUrls)
    }

    @Test
    fun testRequestAudioPostUpload() = runBlocking {
        // Add song directly into database
        val songId = transaction {
            SongEntity.new {
                ownerId = EntityID(testUserId, UserTable)
                title = testSong1.title!!
                genres = testSong1.genres!!.toTypedArray()
                moods = testSong1.moods!!.toTypedArray()
                coverArtUrl = testSong1.coverArtUrl
                description = testSong1.description
                credits = testSong1.credits
                duration = testSong1.duration
                streamUrl = testSong1.streamUrl
                nftPolicyId = testSong1.nftPolicyId
                nftName = testSong1.nftName
                mintingStatus = testSong1.mintingStatus!!
                marketplaceStatus = testSong1.marketplaceStatus!!
            }
        }.id.value

        // Request upload
        val response = client.post("v1/songs/$songId/upload") {
            bearerAuth(testUserToken)
            contentType(ContentType.Application.Json)
            setBody(UploadAudioRequest("test1.mp3"))
        }
        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        val resp = response.body<UploadAudioPostResponse>()


        // create a new HttpClient to work around loop back network in test environment
        val localClient = HttpClient()

        val environment: ApplicationEnvironment by inject()
        val config = environment.getConfigChild("aws.s3.audio")
        val file = File(config.getString("smallSongFile"))
        val form = formData {
            resp.fields.forEach {
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


        val formResp = localClient.submitFormWithBinaryData(form) {
            url(resp.url)
            headers {
                append("Accept", ContentType.Application.Json)
            }
        }

        assertThat(formResp.status.value).isIn(200..204)
    }

    @Test
    fun testRequestAudioPostUploadTooLarge() = runBlocking {
        // Add song directly into database
        val songId = transaction {
            SongEntity.new {
                ownerId = EntityID(testUserId, UserTable)
                title = testSong1.title!!
                genres = testSong1.genres!!.toTypedArray()
                moods = testSong1.moods!!.toTypedArray()
                coverArtUrl = testSong1.coverArtUrl
                description = testSong1.description
                credits = testSong1.credits
                duration = testSong1.duration
                streamUrl = testSong1.streamUrl
                nftPolicyId = testSong1.nftPolicyId
                nftName = testSong1.nftName
                mintingStatus = testSong1.mintingStatus!!
                marketplaceStatus = testSong1.marketplaceStatus!!
            }
        }.id.value

        // Request upload
        val response = client.post("v1/songs/$songId/upload") {
            bearerAuth(testUserToken)
            contentType(ContentType.Application.Json)
            setBody(UploadAudioRequest("test1.mp3"))
        }
        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        val resp = response.body<UploadAudioPostResponse>()

        // create a new HttpClient to work around loop back network in test environment
        val localClient = HttpClient()

        val environment: ApplicationEnvironment by inject()
        val config = environment.getConfigChild("aws.s3.audio")
        val file = File(config.getString("bigSongFile"))
        val form = formData {
            resp.fields.forEach {
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


        val result = runCatching {
            localClient.submitFormWithBinaryData(form) {
                url(resp.url)
                headers {
                    append("Accept", ContentType.Application.Json)
                }
            }
        }.onFailure {
            assertThat(it).isInstanceOf(EOFException::class.java)
        }
        assertThat(result.isFailure).isTrue()
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
                        this.genres = arrayOf(genre)
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
                        this.genres = arrayOf(genre)
                    }
                }
            }
            // Owner 2 - this is what we should filter out
            for (i in (frequency + 1)..expectedGenres.size) {
                transaction {
                    SongEntity.new {
                        this.ownerId = EntityID(ownerId2, UserTable)
                        title = "title_${genre}_$i"
                        this.genres = arrayOf(genre)
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
                parameter("ownerIds", ownerId1)
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
    fun testGetSongCount() = runBlocking {
        var count = 0L
        while (true) {
            val response = client.get("v1/songs/count") {
                bearerAuth(testUserToken)
            }
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val actualCount = response.body<CountResponse>().count
            assertThat(actualCount).isEqualTo(count)

            if (++count == 10L) break

            transaction {
                SongEntity.new {
                    this.ownerId = EntityID(testUserId, UserTable)
                    title = "song$count"
                    genres = arrayOf("genre")
                }
            }
        }
    }

    @Test
    fun testGetSongGenreCount() = runBlocking {
        var count = 0L
        while (true) {
            val response = client.get("v1/songs/genres/count") {
                bearerAuth(testUserToken)
            }
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val actualCount = response.body<CountResponse>().count
            assertThat(actualCount).isEqualTo(count)

            if (++count == 10L) break

            transaction {
                SongEntity.new {
                    this.ownerId = EntityID(testUserId, UserTable)
                    title = "song$count"
                    genres = arrayOf("genre$count")
                }
            }
        }
    }

    @Test
    fun testGetMintingPaymentAmount() = runBlocking {
        // Add mint price value to database directly
        val expectedAmount = 6000000
        transaction {
            exec("INSERT INTO config VALUES ('mint.price','$expectedAmount')")
        }

        // Add Song directly into database
        val songId = transaction {
            SongEntity.new {
                ownerId = EntityID(testUserId, UserTable)
                title = testSong1.title!!
                genres = testSong1.genres!!.toTypedArray()
                coverArtUrl = testSong1.coverArtUrl
                description = testSong1.description
                credits = testSong1.credits
            }
        }.id.value

        // get required payment amount
        val response = client.get("v1/songs/$songId/mint/payment") {
            bearerAuth(testUserToken)
        }
        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        val actualCborHex = response.body<MintPaymentResponse>().cborHex
        val expectedCborHex = CborInteger.create(expectedAmount).toCborByteArray().toHexString()
        assertThat(actualCborHex).isEqualTo(expectedCborHex)
    }

// TODO: complete implementation of testGenerateMintingPaymentTransaction() bellow
    /*
    @Test
    fun testGenerateMintingPaymentTransaction() = runBlocking {

        // Add mint price value to database directly
        val expectedAmount = 6000000
        transaction {
            exec("INSERT INTO config VALUES ('mint.price','$expectedAmount')")
        }

        // Add Song directly into database
        val songId = transaction {
            SongEntity.new {
                ownerId = EntityID(testUserId, UserTable)
                title = testSong1.title!!
                genres = testSong1.genres!!.toTypedArray()
                coverArtUrl = testSong1.coverArtUrl
                description = testSong1.description
                credits = testSong1.credits
            }
        }.id.value

        // generate transaction
        val changeAddress = "addr_test1vrdqad0dwcg5stk9pzdknkrsurzkc8z9rqp9vyfrnrsgxkc4r8za2"
        val response = client.post("v1/songs/$songId/mint/payment") {
            bearerAuth(testUserToken)
            contentType(ContentType.Application.Json)
            setBody(MintPaymentRequest(changeAddress, listOf(????)) // TODO: figure out
        }
        assertThat(response.status).isEqualTo(HttpStatusCode.OK)

        val actualCborHex = response.body<MintPaymentResponse>().cborHex
        val expectedCborHex = ????  // TODO: figure out
        assertThat(actualCborHex).isEqualTo(expectedCborHex)
    }
     */
}
