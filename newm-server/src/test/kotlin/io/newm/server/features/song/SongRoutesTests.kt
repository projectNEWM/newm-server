package io.newm.server.features.song

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
import io.newm.shared.koin.inject
import io.newm.shared.ktx.existsHavingId
import io.newm.shared.ktx.getConfigChild
import io.newm.shared.ktx.getConfigString
import io.newm.shared.ktx.getString
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.EOFException
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class SongRoutesTests : BaseApplicationTests() {

    @BeforeEach
    fun beforeEach() {
        transaction {
            SongTable.deleteAll()
            KeyTable.deleteAll()
            UserTable.deleteWhere { id neq testUserId }
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
        assertThat(song.album).isEqualTo(testSong1.album)
        assertThat(song.track).isEqualTo(testSong1.track)
        assertThat(song.language).isEqualTo(testSong1.language)
        assertThat(song.copyright).isEqualTo(testSong1.copyright)
        assertThat(song.parentalAdvisory).isEqualTo(testSong1.parentalAdvisory)
        assertThat(song.barcodeType).isEqualTo(testSong1.barcodeType)
        assertThat(song.barcodeNumber).isEqualTo(testSong1.barcodeNumber)
        assertThat(song.isrc).isEqualTo(testSong1.isrc)
        assertThat(song.ipi).isEqualTo(testSong1.ipi)
        assertThat(song.releaseDate).isEqualTo(testSong1.releaseDate)
        assertThat(song.lyricsUrl).isEqualTo(testSong1.lyricsUrl)
        assertThat(song.mintingStatus).isEqualTo(MintingStatus.Undistributed)
        assertThat(song.marketplaceStatus).isEqualTo(MarketplaceStatus.NotSelling)
    }

    @Test
    fun testGetSong() = runBlocking {
        // Add Song directly into database
        val song = addSongToDatabase()

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
            expectedSongs += addSongToDatabase(offset)
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
        // Add Songs directly into database
        val allSongs = mutableListOf<Song>()
        for (offset in 0..30) {
            allSongs += addSongToDatabase(offset)
        }

        // filter out 1st and last
        val expectedSongs = allSongs.subList(1, allSongs.size - 1)
        val ids = expectedSongs.joinToString { it.id.toString() }

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
        // Add Songs directly into database
        val allSongs = mutableListOf<Song>()
        for (offset in 0..30) {
            allSongs += addSongToDatabase(offset)
        }

        // filter out 1st and last
        val expectedSongs = allSongs.subList(1, allSongs.size - 1)
        val ownerIds = expectedSongs.joinToString { it.ownerId.toString() }

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
        // Add Songs directly into database
        val allSongs = mutableListOf<Song>()
        for (offset in 0..30) {
            allSongs += addSongToDatabase(offset)
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
        // Add Songs directly into database
        val allSongs = mutableListOf<Song>()
        for (offset in 0..30) {
            allSongs += addSongToDatabase(offset)
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
    fun testGetSongsByMintingStatus() = runBlocking {
        // Add Songs directly into database
        val allSongs = mutableListOf<Song>()
        for (offset in 0..30) {
            allSongs += addSongToDatabase(offset)
        }

        for (expectedMintingStatus in MintingStatus.values()) {
            // filter out
            val expectedSongs = allSongs.filter { it.mintingStatus == expectedMintingStatus }

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
                    parameter("mintingStatuses", expectedMintingStatus)
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
    }

    @Test
    fun testGetSongsByOlderThan() = runBlocking {
        // Add Songs directly into database
        val allSongs = mutableListOf<Song>()
        for (offset in 0..30) {
            allSongs += addSongToDatabase(offset)
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
        // Add Songs directly into database
        val allSongs = mutableListOf<Song>()
        for (offset in 0..30) {
            allSongs += addSongToDatabase(offset)
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

        // Add Songs directly into database
        val allSongs = mutableListOf<Song>()
        for (offset in 0..30) {
            allSongs += addSongToDatabase(offset = offset, phrase = phrase.takeIf { offset % 2 == 0 })
        }

        // filter out for phrase
        val expectedSongs = allSongs.filter {
            phrase in it.title!! || phrase in it.description!! || phrase in it.album!! || phrase in it.nftName!!
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
        // Add Song directly into database
        val song1 = addSongToDatabase(ownerId = testUserId)
        val songId = song1.id!!

        // Patch it with Song2
        val response = client.patch("v1/songs/$songId") {
            bearerAuth(testUserToken)
            contentType(ContentType.Application.Json)
            setBody(testSong2)
        }
        assertThat(response.status).isEqualTo(HttpStatusCode.NoContent)

        // Read Song directly from database & verify it
        val song2 = transaction { SongEntity[songId] }.toModel()
        assertThat(song2.id).isEqualTo(songId)
        assertThat(song2.ownerId).isEqualTo(testUserId)
        assertThat(song2.createdAt).isAtLeast(song1.createdAt)
        assertThat(song2.title).isEqualTo(testSong2.title)
        assertThat(song2.genres).isEqualTo(testSong2.genres)
        assertThat(song2.moods).isEqualTo(testSong2.moods)
        assertThat(song2.description).isEqualTo(testSong2.description)
        assertThat(song2.album).isEqualTo(testSong2.album)
        assertThat(song2.track).isEqualTo(testSong2.track)
        assertThat(song2.language).isEqualTo(testSong2.language)
        assertThat(song2.copyright).isEqualTo(testSong2.copyright)
        assertThat(song2.parentalAdvisory).isEqualTo(testSong2.parentalAdvisory)
        assertThat(song2.barcodeType).isEqualTo(testSong2.barcodeType)
        assertThat(song2.barcodeNumber).isEqualTo(testSong2.barcodeNumber)
        assertThat(song2.isrc).isEqualTo(testSong2.isrc)
        assertThat(song2.ipi).isEqualTo(testSong2.ipi)
        assertThat(song2.releaseDate).isEqualTo(testSong2.releaseDate)
        assertThat(song2.lyricsUrl).isEqualTo(testSong2.lyricsUrl)
        assertThat(song2.mintingStatus).isEqualTo(MintingStatus.Undistributed)
        assertThat(song2.marketplaceStatus).isEqualTo(MarketplaceStatus.NotSelling)
    }

    @Test
    fun testDeleteSong() = runBlocking {
        // Add song directly into database
        val songId = addSongToDatabase(ownerId = testUserId).id!!

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
        val environment: ApplicationEnvironment by inject()
        val region = environment.getConfigString("aws.region")
        val bucketName = environment.getConfigString("aws.s3.audio.bucketName")
        val bucketUrl = "https://$bucketName.s3.$region.amazonaws.com"
        val songId = addSongToDatabase(ownerId = testUserId).id!!
        val fileName = "test1.wav"
        val key = "$songId/$fileName"

        // Request upload
        val response = client.post("v1/songs/$songId/upload") {
            bearerAuth(testUserToken)
            contentType(ContentType.Application.Json)
            setBody(AudioUploadRequest(fileName))
        }
        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        with(response.body<AudioUploadResponse>()) {
            assertThat(url).isEqualTo(bucketUrl)
            assertThat(fields.size).isEqualTo(7)
            assertThat(fields["bucket"]).isEqualTo(bucketName)
            assertThat(fields["key"]).isEqualTo(key)
            assertThat(fields["X-Amz-Algorithm"]).isEqualTo("AWS4-HMAC-SHA256")
            assertThat(fields).containsKey("X-Amz-Credential")
            assertThat(fields).containsKey("X-Amz-Date")
            assertThat(fields).containsKey("policy")
            assertThat(fields).containsKey("X-Amz-Signature")
        }

        val expectedAudioUrl = "s3://$bucketName/$key"
        val actualAudioUrl = transaction { SongEntity[songId].originalAudioUrl }
        assertThat(actualAudioUrl).isEqualTo(expectedAudioUrl)
    }

    @Disabled("Disabled because this test requires real AWS credentials")
    @Test
    fun testRequestAudioPostUpload() = runBlocking {
        // Add song directly into database
        val songId = addSongToDatabase(ownerId = testUserId).id!!

        // Request upload
        val response = client.post("v1/songs/$songId/upload") {
            bearerAuth(testUserToken)
            contentType(ContentType.Application.Json)
            setBody(AudioUploadRequest("test1.mp3"))
        }
        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        val resp = response.body<AudioUploadResponse>()

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

    @Disabled("Disabled because this test requires real AWS credentials")
    @Test
    fun testRequestAudioPostUploadTooLarge() = runBlocking {
        // Add song directly into database
        val songId = addSongToDatabase(ownerId = testUserId).id!!

        // Request upload
        val response = client.post("v1/songs/$songId/upload") {
            bearerAuth(testUserToken)
            contentType(ContentType.Application.Json)
            setBody(AudioUploadRequest("test1.mp3"))
        }
        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        val resp = response.body<AudioUploadResponse>()

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
        val songId = addSongToDatabase(ownerId = testUserId).id!!

        // get required payment amount
        val response = client.get("v1/songs/$songId/mint/payment") {
            bearerAuth(testUserToken)
        }
        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        val actualCborHex = response.body<MintPaymentResponse>().cborHex
        val expectedCborHex = CborInteger.create(expectedAmount).toCborByteArray().toHexString()
        assertThat(actualCborHex).isEqualTo(expectedCborHex)
    }

    @Test
    fun testGetStreamMetatdata() = runBlocking {
        // Add song directly into database
        val streamId = UUID.randomUUID().toString()
        val songId = addSongToDatabase(ownerId = testUserId, init = {
            streamUrl = "https://newm.io/$streamId/$streamId.m3u8"
        }).id!!

        // Fetch stream metadata
        val response = client.get("v1/songs/$songId/stream") {
            bearerAuth(testUserToken)
            contentType(ContentType.Application.Json)
        }
        assertThat(response.status).isEqualTo(HttpStatusCode.OK)

        val resp = response.body<AudioStreamResponse>()
        // assert that URL was updated
        val environment: ApplicationEnvironment by inject()
        val updatedHost = environment.getConfigString("aws.cloudFront.audioStream.hostUrl")
        assertThat(resp.url).startsWith(updatedHost)
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
        // Add song directly into database
        val songId = addSongToDatabase(ownerId = testUserId).id!!

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

fun addSongToDatabase(offset: Int = 0, ownerId: UUID? = null, phrase: String? = null, init: (SongEntity.() -> Unit)? = null): Song {
    val ownerEntityId = ownerId?.let {
        EntityID(it, UserTable)
    } ?: transaction {
        UserEntity.new {
            email = "artist$offset@newm.io"
        }
    }.id

    val paymentKeyId = transaction {
        KeyEntity.new {
            this.address = ""
            this.vkey = ""
            this.skey = ""
        }
    }.id

    fun phraseOrBlank(offset: Int, target: Int) = phrase?.takeIf { offset % 4 == target }.orEmpty()

    return transaction {
        SongEntity.new {
            this.ownerId = ownerEntityId
            title = "title$offset ${phraseOrBlank(offset, 0)} blah blah"
            description = "description$offset ${phraseOrBlank(offset, 1)} blah blah"
            album = "album$offset ${phraseOrBlank(offset, 2)} blah blah"
            nftName = "nftName$offset ${phraseOrBlank(offset, 3)} blah blah"
            genres = arrayOf("genre${offset}_0", "genre${offset}_1")
            moods = arrayOf("mood${offset}_0", "mood${offset}_1")
            coverArtUrl = "https://newm.io/cover$offset"
            track = offset
            language = "language$offset"
            copyright = "copyright$offset"
            parentalAdvisory = "parentalAdvisory$offset"
            barcodeType = SongBarcodeType.values()[offset % SongBarcodeType.values().size]
            barcodeNumber = "barcodeNumber$offset"
            isrc = "isrc$offset"
            ipi = arrayOf("ipi${offset}_0", "ipi${offset}_1")
            releaseDate = LocalDate.of(2023, 1, offset % 31 + 1)
            publicationDate = LocalDate.of(2023, 1, offset % 31 + 1)
            lyricsUrl = "https://newm.io/lyrics$offset"
            tokenAgreementUrl = "https://newm.io/agreement$offset"
            originalAudioUrl = "https://newm.io/audio$offset"
            clipUrl = "https://newm.io/clip$offset"
            streamUrl = "https://newm.io/stream$offset"
            duration = offset
            nftPolicyId = "nftPolicyId$offset"
            mintingStatus = MintingStatus.values()[offset % MintingStatus.values().size]
            marketplaceStatus = MarketplaceStatus.values()[offset % MarketplaceStatus.values().size]
            this.paymentKeyId = paymentKeyId
            if (init != null) {
                this.apply { init() }
            }
        }
    }.toModel()
}
