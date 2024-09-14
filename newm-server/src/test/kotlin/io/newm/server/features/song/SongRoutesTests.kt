package io.newm.server.features.song

import com.google.common.truth.Truth.assertThat
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.setCookie
import io.ktor.server.application.ApplicationEnvironment
import io.mockk.coEvery
import io.mockk.mockk
import io.newm.server.BaseApplicationTests
import io.newm.server.features.cardano.database.KeyEntity
import io.newm.server.features.cardano.database.KeyTable
import io.newm.server.features.cardano.repo.CardanoRepository
import io.newm.server.features.model.CountResponse
import io.newm.server.features.song.database.ReleaseEntity
import io.newm.server.features.song.database.ReleaseTable
import io.newm.server.features.song.database.SongEntity
import io.newm.server.features.song.database.SongReceiptTable
import io.newm.server.features.song.database.SongTable
import io.newm.server.features.song.model.AudioEncodingStatus
import io.newm.server.features.song.model.AudioStreamResponse
import io.newm.server.features.song.model.AudioUploadReport
import io.newm.server.features.song.model.MarketplaceStatus
import io.newm.server.features.song.model.MintPaymentResponse
import io.newm.server.features.song.model.MintingStatus
import io.newm.server.features.song.model.ReleaseBarcodeType
import io.newm.server.features.song.model.ReleaseType
import io.newm.server.features.song.model.Song
import io.newm.server.features.song.model.SongIdBody
import io.newm.server.features.song.repo.SongRepository
import io.newm.server.features.song.repo.SongRepositoryImpl
import io.newm.server.features.user.database.UserEntity
import io.newm.server.features.user.database.UserTable
import io.newm.server.typealiases.UserId
import io.newm.server.utils.ResourceOutgoingContent
import io.newm.shared.koin.inject
import io.newm.shared.ktx.existsHavingId
import io.newm.shared.ktx.getConfigString
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.GlobalContext.loadKoinModules
import org.koin.dsl.module
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class SongRoutesTests : BaseApplicationTests() {
    @BeforeAll
    fun beforeAllSongRoutesTests() {
        val module = module {
            single<SongRepository> {
                SongRepositoryImpl(
                    get(),
                    get(),
                    get(),
                    // we need to mock the CardanoRepository to have a fixed ada price
                    mockk<CardanoRepository>(relaxed = true) {
                        coEvery { queryAdaUSDPrice() } returns 253400L // $0.2534 ada price
                    },
                    get(),
                    get(),
                    get()
                )
            }
        }
        loadKoinModules(module)
    }

    @BeforeEach
    fun beforeEach() {
        transaction {
            SongReceiptTable.deleteAll()
            SongTable.deleteAll()
            ReleaseTable.deleteAll()
            KeyTable.deleteAll()
            UserTable.deleteWhere { id neq testUserId }
        }
    }

    @Test
    fun testPostSong() =
        runBlocking {
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
            val song = transaction { SongEntity[songId].let { it.toModel(ReleaseEntity[it.releaseId!!].toModel()) } }
            assertThat(song.id).isEqualTo(songId)
            assertThat(song.archived).isEqualTo(false)
            assertThat(song.ownerId).isEqualTo(testUserId)
            assertThat(song.createdAt).isAtLeast(startTime)
            assertThat(song.title).isEqualTo(testSong1.title)
            assertThat(song.genres).isEqualTo(testSong1.genres)
            assertThat(song.moods).isEqualTo(testSong1.moods)
            assertThat(song.description).isEqualTo(testSong1.description)
            assertThat(song.track).isEqualTo(testSong1.track)
            assertThat(song.language).isEqualTo(testSong1.language)
            assertThat(song.coverRemixSample).isEqualTo(testSong1.coverRemixSample)
            assertThat(song.compositionCopyrightOwner).isEqualTo(testSong1.compositionCopyrightOwner)
            assertThat(song.compositionCopyrightYear).isEqualTo(testSong1.compositionCopyrightYear)
            assertThat(song.phonographicCopyrightOwner).isEqualTo(testSong1.phonographicCopyrightOwner)
            assertThat(song.phonographicCopyrightYear).isEqualTo(testSong1.phonographicCopyrightYear)
            assertThat(song.parentalAdvisory).isEqualTo(testSong1.parentalAdvisory)
            assertThat(song.barcodeType).isEqualTo(testSong1.barcodeType)
            assertThat(song.barcodeNumber).isEqualTo(testSong1.barcodeNumber)
            assertThat(song.isrc).isEqualTo(testSong1.isrc)
            assertThat(song.ipis).isEqualTo(testSong1.ipis)
            assertThat(song.releaseDate).isEqualTo(testSong1.releaseDate)
            assertThat(song.publicationDate).isEqualTo(testSong1.publicationDate)
            assertThat(song.lyricsUrl).isEqualTo(testSong1.lyricsUrl)
            assertThat(song.audioEncodingStatus).isEqualTo(AudioEncodingStatus.NotStarted)
            assertThat(song.mintingStatus).isEqualTo(MintingStatus.Undistributed)
            assertThat(song.marketplaceStatus).isEqualTo(MarketplaceStatus.NotSelling)
        }

    @Test
    fun testGetSong() =
        runBlocking {
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
    fun testGetAllSongs() =
        runBlocking {
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
    fun testGetAllSongsInDescendingOrder() =
        runBlocking {
            // Add Songs directly into database
            val allSongs = mutableListOf<Song>()
            for (offset in 0..30) {
                allSongs += addSongToDatabase(offset)
            }
            val expectedSongs = allSongs.sortedByDescending { it.createdAt }

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
                    parameter("sortOrder", "desc")
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
    fun testGetNonArchivedSongsImplicitly() =
        runBlocking {
            // Add Songs directly into database
            val allSongs = mutableListOf<Song>()
            for (offset in 0..30) {
                allSongs += addSongToDatabase(offset, archived = (offset % 2) == 0)
            }

            // filter out archived
            val expectedSongs = allSongs.filter { it.archived == false }

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
    fun testGetNonArchivedSongsExplicitly() =
        runBlocking {
            // Add Songs directly into database
            val allSongs = mutableListOf<Song>()
            for (offset in 0..30) {
                allSongs += addSongToDatabase(offset, archived = (offset % 2) == 0)
            }

            // filter out archived
            val expectedSongs = allSongs.filter { it.archived == false }

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
                    parameter("archived", false)
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
    fun testGetArchivedSongs() =
        runBlocking {
            // Add Songs directly into database
            val allSongs = mutableListOf<Song>()
            for (offset in 0..30) {
                allSongs += addSongToDatabase(offset, archived = (offset % 2) == 0)
            }

            // filter out non-archived
            val expectedSongs = allSongs.filter { it.archived == true }

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
                    parameter("archived", true)
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
    fun testGetSongsByIds() =
        runBlocking {
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
    fun testGetSongsByIdsExclusion() =
        runBlocking {
            // Add Songs directly into database
            val allSongs = mutableListOf<Song>()
            for (offset in 0..30) {
                allSongs += addSongToDatabase(offset)
            }

            // filter out 1st and last
            val expectedSongs = allSongs.subList(1, allSongs.size - 1)
            val ids = allSongs.filter { it !in expectedSongs }.joinToString { "-${it.id}" }

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
    fun testGetSongsByOwnerIds() =
        runBlocking {
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
    fun testGetSongsByOwnerIdsExclusion() =
        runBlocking {
            // Add Songs directly into database
            val allSongs = mutableListOf<Song>()
            for (offset in 0..30) {
                allSongs += addSongToDatabase(offset)
            }

            // filter out 1st and last
            val expectedSongs = allSongs.subList(1, allSongs.size - 1)
            val ownerIds = allSongs.filter { it !in expectedSongs }.joinToString { "-${it.ownerId}" }

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
    fun testGetSongsByGenres() =
        runBlocking {
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
    fun testGetSongsByGenresExclusion() =
        runBlocking {
            // Add Songs directly into database
            val allSongs = mutableListOf<Song>()
            for (offset in 0..30) {
                allSongs += addSongToDatabase(offset)
            }

            // filter out 1st and last and take only 1st genre of each
            val expectedSongs = allSongs.subList(1, allSongs.size - 1)
            val genres = allSongs.filter { it !in expectedSongs }.joinToString { "-${it.genres!!.first()}" }

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
    fun testGetSongsByMoods() =
        runBlocking {
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
    fun testGetSongsByMoodsExclusion() =
        runBlocking {
            // Add Songs directly into database
            val allSongs = mutableListOf<Song>()
            for (offset in 0..30) {
                allSongs += addSongToDatabase(offset)
            }

            // filter out 1st and last and take only 1st mood of each
            val expectedSongs = allSongs.subList(1, allSongs.size - 1)
            val moods = allSongs.filter { it !in expectedSongs }.joinToString { "-${it.moods!!.first()}" }

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
    fun testGetSongsByMintingStatus() =
        runBlocking {
            // Add Songs directly into database
            val allSongs = mutableListOf<Song>()
            for (offset in 0..30) {
                allSongs += addSongToDatabase(offset)
            }

            for (expectedMintingStatus in MintingStatus.entries) {
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
    fun testGetSongsByMintingStatusExclusion() =
        runBlocking {
            // Add Songs directly into database
            val allSongs = mutableListOf<Song>()
            for (offset in 0..30) {
                allSongs += addSongToDatabase(offset)
            }

            for (expectedMintingStatus in MintingStatus.entries) {
                // filter out
                val expectedSongs = allSongs.filter { it.mintingStatus != expectedMintingStatus }

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
                        parameter("mintingStatuses", "-$expectedMintingStatus")
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
    fun testGetSongsByOlderThan() =
        runBlocking {
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
    fun testGetSongsByNewerThan() =
        runBlocking {
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
    fun testGetSongsByPhrase() =
        runBlocking {
            val phrase = "ABCDE"

            // Add Songs directly into database
            val allSongs = mutableListOf<Song>()
            for (offset in 0..30) {
                allSongs += addSongToDatabase(offset = offset, phrase = phrase.takeIf { offset % 2 == 0 })
            }

            // filter out for phrase
            val expectedSongs = allSongs.filter { song ->
                val stageOrFullName = transaction { UserEntity[song.ownerId!!].stageOrFullName }
                phrase in song.title!! ||
                    phrase in song.description!! ||
                    phrase in song.nftName!! ||
                    phrase in stageOrFullName
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
    fun testPatchSong() =
        runBlocking {
            // Add Song directly into database
            val song1 = addSongToDatabase(ownerId = testUserId) {
                ReleaseEntity[releaseId!!].hasSubmittedForDistribution = true
            }
            val songId = song1.id!!

            // Patch it with Song2
            val response = client.patch("v1/songs/$songId") {
                bearerAuth(testUserToken)
                contentType(ContentType.Application.Json)
                setBody(testSong2)
            }
            assertThat(response.status).isEqualTo(HttpStatusCode.NoContent)

            // Read Song directly from database & verify it
            val song2 = transaction {
                val song = SongEntity[songId]
                val release = ReleaseEntity[song.releaseId!!].toModel()
                song.toModel(release)
            }
            assertThat(song2.id).isEqualTo(songId)
            assertThat(song2.archived).isEqualTo(testSong2.archived)
            assertThat(song2.ownerId).isEqualTo(testUserId)
            assertThat(song2.createdAt).isAtLeast(song1.createdAt)
            assertThat(song2.title).isEqualTo(testSong2.title)
            assertThat(song2.genres).isEqualTo(testSong2.genres)
            assertThat(song2.moods).isEqualTo(testSong2.moods)
            assertThat(song2.description).isEqualTo(testSong2.description)
            assertThat(song2.track).isEqualTo(testSong2.track)
            assertThat(song2.language).isEqualTo(testSong2.language)
            assertThat(song2.coverRemixSample).isEqualTo(testSong2.coverRemixSample)
            assertThat(song2.compositionCopyrightOwner).isEqualTo(testSong2.compositionCopyrightOwner)
            assertThat(song2.compositionCopyrightYear).isEqualTo(testSong2.compositionCopyrightYear)
            assertThat(song2.phonographicCopyrightOwner).isEqualTo(testSong2.phonographicCopyrightOwner)
            assertThat(song2.phonographicCopyrightYear).isEqualTo(testSong2.phonographicCopyrightYear)
            assertThat(song2.parentalAdvisory).isEqualTo(testSong2.parentalAdvisory)
            assertThat(song2.barcodeType).isEqualTo(testSong2.barcodeType)
            assertThat(song2.barcodeNumber).isEqualTo(testSong2.barcodeNumber)
            assertThat(song2.isrc).isEqualTo(testSong2.isrc)
            assertThat(song2.ipis).isEqualTo(testSong2.ipis)
            // releaseDate should not be updated so check against song1.
            assertThat(song2.releaseDate).isEqualTo(testSong1.releaseDate)
            assertThat(song2.publicationDate).isEqualTo(testSong2.publicationDate)
            assertThat(song2.lyricsUrl).isEqualTo(testSong2.lyricsUrl)
            assertThat(song2.audioEncodingStatus).isEqualTo(AudioEncodingStatus.NotStarted)
            assertThat(song2.mintingStatus).isEqualTo(MintingStatus.Undistributed)
            assertThat(song2.marketplaceStatus).isEqualTo(MarketplaceStatus.NotSelling)
        }

    @Test
    fun testDeleteSong() =
        runBlocking {
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
    fun testUploadSongAudio() =
        runBlocking {
            val environment: ApplicationEnvironment by inject()
            val bucketName = environment.getConfigString("aws.s3.audio.bucketName")
            val songId = addSongToDatabase(ownerId = testUserId).id!!

            // Request upload
            val response = client.post("v1/songs/$songId/audio") {
                bearerAuth(testUserToken)
                contentType(ContentType.Application.OctetStream) // intentionally not set to "audio/x-flac" to make sure we detect
                setBody(ResourceOutgoingContent("sample1.flac"))
            }
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val expectedAudioUrl = "s3://$bucketName/$songId/audio.flac"
            with(response.body<AudioUploadReport>()) {
                assertThat(url).isEqualTo(expectedAudioUrl)
                assertThat(mimeType).isEqualTo("audio/x-flac")
                assertThat(fileSize).isEqualTo(12358748)
                assertThat(duration).isEqualTo(122)
                assertThat(sampleRate).isEqualTo(44100)
            }

            val actualAudioUrl = transaction { SongEntity[songId].originalAudioUrl }
            assertThat(actualAudioUrl).isEqualTo(expectedAudioUrl)
        }

    @Test
    fun testGetAllGenres() =
        runBlocking {
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
    fun testGetGenresByOwner() =
        runBlocking {
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
    fun testGetSongCount() =
        runBlocking {
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
    fun testGetSongGenreCount() =
        runBlocking {
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
    fun testGetMintingPaymentAmount() =
        runBlocking {
            // Add mint price value to database directly
            val expectedAmount = 2000000
            transaction {
                exec("INSERT INTO config VALUES ('mint.price','$expectedAmount')")
                exec("INSERT INTO config VALUES ('distribution.price.usd','14990000')")
            }

            // Add Song directly into database
            val songId = addSongToDatabase(ownerId = testUserId).id!!

            // get required payment amount
            val response = client.get("v1/songs/$songId/mint/payment") {
                bearerAuth(testUserToken)
            }
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val responseBody: MintPaymentResponse = response.body()
            val actualCborHex = responseBody.cborHex
            val expectedCborHex = "1a03b46ade"
            assertThat(actualCborHex).isEqualTo(expectedCborHex)
            assertThat(responseBody.usdPrice).isEqualTo("15.496800") // $15.4968
        }

    @Test
    fun testGetStreamMetatdata() =
        runBlocking {
            // Add song directly into database
            val streamId = UUID.randomUUID().toString()
            val songId = addSongToDatabase(ownerId = testUserId, init = {
                streamUrl = "https://newm.io/$streamId/$streamId.m3u8"
            }).id!!

            // Fetch stream metadata
            val response = client.get("v1/songs/$songId/stream") {
                bearerAuth(testUserToken)
                contentType(ContentType.Application.Json)
                url {
                    protocol = URLProtocol.HTTPS
                }
            }
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)

            val resp = response.body<AudioStreamResponse>()
            // assert that URL was updated
            val environment: ApplicationEnvironment by inject()
            val updatedHost = environment.getConfigString("aws.cloudFront.audioStream.hostUrl")
            assertThat(resp.url).startsWith(updatedHost)

            // assert that cookies are created
            val cookies = response.setCookie()
            assertThat(cookies).isNotEmpty()
            assertThat(cookies.filter { it.name == "CloudFront-Key-Pair-Id" }).isNotEmpty()
            assertThat(cookies.filter { it.name == "CloudFront-Signature" }).isNotEmpty()
            assertThat(cookies.filter { it.name == "CloudFront-Policy" }).isNotEmpty()
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

fun addSongToDatabase(
    offset: Int = 0,
    ownerId: UserId? = null,
    archived: Boolean = false,
    phrase: String? = null,
    init: (SongEntity.() -> Unit)? = null
): Song {
    fun phraseOrEmpty(target: Int) = phrase?.takeIf { offset % 7 == target }.orEmpty()

    val ownerEntityId = ownerId?.let {
        EntityID(it, UserTable)
    } ?: transaction {
        UserEntity.new {
            email = "artist$offset@newm.io"
            if (offset % 4 == 0) {
                nickname = "nickname$offset ${phraseOrEmpty(0)} blah blah"
            }
            firstName = "firstName$offset ${phraseOrEmpty(1)} blah blah"
            lastName = "lastName$offset ${phraseOrEmpty(2)} blah blah"
        }
    }.id

    val paymentKeyId = transaction {
        KeyEntity.new {
            this.address = ""
            this.vkey = ""
            this.skey = ""
        }
    }.id

    val title = "title$offset ${phraseOrEmpty(3)} blah blah"
    val releaseId = transaction {
        ReleaseEntity
            .new {
                this.title = title
                this.ownerId = ownerEntityId
                releaseType = ReleaseType.SINGLE
                coverArtUrl = "https://newm.io/cover$offset"
                barcodeType = ReleaseBarcodeType.entries[offset % ReleaseBarcodeType.entries.size]
                barcodeNumber = "barcodeNumber$offset"
                releaseDate = LocalDate.of(2023, 1, offset % 31 + 1)
                publicationDate = LocalDate.of(2023, 1, offset % 31 + 1)
                mintCostLovelace = offset.toLong()
            }.id.value
    }
    val release = transaction { ReleaseEntity[releaseId].toModel() }
    return transaction {
        SongEntity.new {
            this.archived = archived
            this.ownerId = ownerEntityId
            this.title = title
            this.releaseId = EntityID(release.id!!, ReleaseTable)
            description = "description$offset ${phraseOrEmpty(4)} blah blah"
            nftName = "nftName$offset ${phraseOrEmpty(6)} blah blah"
            genres = arrayOf("genre${offset}_0", "genre${offset}_1")
            moods = arrayOf("mood${offset}_0", "mood${offset}_1")
            track = offset
            language = "language$offset"
            coverRemixSample = offset % 2 == 0
            compositionCopyrightOwner = "compositionCopyrightOwner$offset"
            compositionCopyrightYear = offset
            phonographicCopyrightOwner = "copyright$phonographicCopyrightOwner"
            phonographicCopyrightYear = 2 * offset
            parentalAdvisory = "parentalAdvisory$offset"
            isrc = "isrc$offset"
            iswc = "iswc$offset"
            ipis = arrayOf("ipi${offset}_0", "ipi${offset}_1")
            lyricsUrl = "https://newm.io/lyrics$offset"
            tokenAgreementUrl = "https://newm.io/agreement$offset"
            originalAudioUrl = "https://newm.io/audio$offset"
            clipUrl = "https://newm.io/clip$offset"
            streamUrl = "https://newm.io/stream$offset"
            duration = offset
            nftPolicyId = "nftPolicyId$offset"
            audioEncodingStatus = AudioEncodingStatus.entries[offset % AudioEncodingStatus.entries.size]
            mintingStatus = MintingStatus.entries[offset % MintingStatus.entries.size]
            marketplaceStatus = MarketplaceStatus.entries[offset % MarketplaceStatus.entries.size]
            this.paymentKeyId = paymentKeyId
            if (init != null) {
                this.apply { init() }
            }
        }
    }.toModel(release)
}
