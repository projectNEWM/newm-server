package io.newm.server.features.playlist

import com.google.common.truth.Truth.assertThat
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.newm.server.BaseApplicationTests
import io.newm.server.features.model.CountResponse
import io.newm.server.features.playlist.database.PlaylistEntity
import io.newm.server.features.playlist.database.PlaylistTable
import io.newm.server.features.playlist.database.SongsInPlaylistsTable
import io.newm.server.features.playlist.model.Playlist
import io.newm.server.features.playlist.model.PlaylistIdBody
import io.newm.server.features.song.addSongToDatabase
import io.newm.server.features.song.database.SongTable
import io.newm.server.features.song.model.Song
import io.newm.server.features.song.model.SongIdBody
import io.newm.server.features.user.database.UserEntity
import io.newm.server.features.user.database.UserTable
import io.newm.server.typealiases.UserId
import io.newm.shared.ktx.exists
import io.newm.shared.ktx.existsHavingId
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class PlaylistRoutesTests : BaseApplicationTests() {
    @BeforeEach
    fun beforeEach() {
        transaction {
            SongsInPlaylistsTable.deleteAll()
            PlaylistTable.deleteAll()
            UserTable.deleteWhere { id neq testUserId }
            SongTable.deleteAll()
        }
    }

    @Test
    fun testPostPlaylist() =
        runBlocking {
            val startTime = LocalDateTime.now()

            // Post
            val response =
                client.post("v1/playlists") {
                    bearerAuth(testUserToken)
                    contentType(ContentType.Application.Json)
                    setBody(testPlaylist1)
                }
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val playlistId = response.body<PlaylistIdBody>().playlistId

            // Read Playlist directly from database & verify it
            val playlist = transaction { PlaylistEntity[playlistId] }.toModel()
            assertThat(playlist.id).isEqualTo(playlistId)
            assertThat(playlist.ownerId).isEqualTo(testUserId)
            assertThat(playlist.createdAt).isAtLeast(startTime)
            assertThat(playlist.name).isEqualTo(testPlaylist1.name)
        }

    @Test
    fun testGetPlaylist() =
        runBlocking {
            // Add Playlist directly into database
            val playlist = addPLaylistToDatabase(ownerId = testUserId)

            // Get it
            val response =
                client.get("v1/playlists/${playlist.id}") {
                    bearerAuth(testUserToken)
                    accept(ContentType.Application.Json)
                }
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            assertThat(response.body<Playlist>()).isEqualTo(playlist)
        }

    @Test
    fun testGetAllPlaylists() =
        runBlocking {
            // Add Playlists directly into database
            val expectedPlaylists = mutableListOf<Playlist>()
            for (offset in 0..30) {
                expectedPlaylists += addPLaylistToDatabase(offset)
            }

            // Get all playlists forcing pagination
            var offset = 0
            val limit = 5
            val actualPlaylists = mutableListOf<Playlist>()
            while (true) {
                val response =
                    client.get("v1/playlists") {
                        bearerAuth(testUserToken)
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val songs = response.body<List<Playlist>>()
                if (songs.isEmpty()) break
                actualPlaylists += songs
                offset += limit
            }

            // verify all
            assertThat(actualPlaylists).isEqualTo(expectedPlaylists)
        }

    @Test
    fun testGetAllPlaylistsInDescendingOrder() =
        runBlocking {
            // Add Playlists directly into database
            val allPlaylists = mutableListOf<Playlist>()
            for (offset in 0..30) {
                allPlaylists += addPLaylistToDatabase(offset)
            }
            val expectedPlaylists = allPlaylists.sortedByDescending { it.createdAt }

            // Get all playlists forcing pagination
            var offset = 0
            val limit = 5
            val actualPlaylists = mutableListOf<Playlist>()
            while (true) {
                val response =
                    client.get("v1/playlists") {
                        bearerAuth(testUserToken)
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                        parameter("sortOrder", "desc")
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val songs = response.body<List<Playlist>>()
                if (songs.isEmpty()) break
                actualPlaylists += songs
                offset += limit
            }

            // verify all
            assertThat(actualPlaylists).isEqualTo(expectedPlaylists)
        }

    @Test
    fun testGetPlaylistsByIds() =
        runBlocking {
            // Add Playlists directly into database
            val allPlaylists = mutableListOf<Playlist>()
            for (offset in 0..30) {
                allPlaylists += addPLaylistToDatabase(offset)
            }

            // filter out 1st and last
            val expectedPlaylists = allPlaylists.subList(1, allPlaylists.size - 1)
            val ids = expectedPlaylists.joinToString { it.id.toString() }

            // Get all playlists forcing pagination
            var offset = 0
            val limit = 5
            val actualPlaylists = mutableListOf<Playlist>()
            while (true) {
                val response =
                    client.get("v1/playlists") {
                        bearerAuth(testUserToken)
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                        parameter("ids", ids)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val songs = response.body<List<Playlist>>()
                if (songs.isEmpty()) break
                actualPlaylists += songs
                offset += limit
            }

            // verify all
            assertThat(actualPlaylists).isEqualTo(expectedPlaylists)
        }

    @Test
    fun testGetPlaylistsByOwnerIds() =
        runBlocking {
            // Add Playlists directly into database
            val allPlaylists = mutableListOf<Playlist>()
            for (offset in 0..30) {
                allPlaylists += addPLaylistToDatabase(offset)
            }

            // filter out 1st and last
            val expectedPlaylists = allPlaylists.subList(1, allPlaylists.size - 1)
            val ownerIds = expectedPlaylists.joinToString { it.ownerId.toString() }

            // Get all playlists forcing pagination
            var offset = 0
            val limit = 5
            val actualPlaylists = mutableListOf<Playlist>()
            while (true) {
                val response =
                    client.get("v1/playlists") {
                        bearerAuth(testUserToken)
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                        parameter("ownerIds", ownerIds)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val songs = response.body<List<Playlist>>()
                if (songs.isEmpty()) break
                actualPlaylists += songs
                offset += limit
            }

            // verify all
            assertThat(actualPlaylists).isEqualTo(expectedPlaylists)
        }

    @Test
    fun testGetPlaylistsByOlderThan() =
        runBlocking {
            // Add Playlists directly into database
            val allPlaylists = mutableListOf<Playlist>()
            for (offset in 0..30) {
                allPlaylists += addPLaylistToDatabase(offset)
            }

            // filter out newest one
            val expectedPlaylists = allPlaylists.subList(0, allPlaylists.size - 1)
            val olderThan = allPlaylists.last().createdAt

            // Get all playlists forcing pagination
            var offset = 0
            val limit = 5
            val actualPlaylists = mutableListOf<Playlist>()
            while (true) {
                val response =
                    client.get("v1/playlists") {
                        bearerAuth(testUserToken)
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                        parameter("olderThan", olderThan)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val songs = response.body<List<Playlist>>()
                if (songs.isEmpty()) break
                actualPlaylists += songs
                offset += limit
            }

            // verify all
            assertThat(actualPlaylists).isEqualTo(expectedPlaylists)
        }

    @Test
    fun testGetPlaylistsByNewerThan() =
        runBlocking {
            // Add Playlists directly into database
            val allPlaylists = mutableListOf<Playlist>()
            for (offset in 0..30) {
                allPlaylists += addPLaylistToDatabase(offset)
            }

            // filter out newest one
            val expectedPlaylists = allPlaylists.subList(1, allPlaylists.size)
            val newerThan = allPlaylists.first().createdAt

            // Get all playlists forcing pagination
            var offset = 0
            val limit = 5
            val actualPlaylists = mutableListOf<Playlist>()
            while (true) {
                val response =
                    client.get("v1/playlists") {
                        bearerAuth(testUserToken)
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                        parameter("newerThan", newerThan)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val songs = response.body<List<Playlist>>()
                if (songs.isEmpty()) break
                actualPlaylists += songs
                offset += limit
            }

            // verify all
            assertThat(actualPlaylists).isEqualTo(expectedPlaylists)
        }

    @Test
    fun testPatchPlaylist() =
        runBlocking {
            // Add Playlist directly into database
            val playlistId = addPLaylistToDatabase(ownerId = testUserId).id!!

            // Patch it with Playlist2
            val response =
                client.patch("v1/playlists/$playlistId") {
                    bearerAuth(testUserToken)
                    contentType(ContentType.Application.Json)
                    setBody(testPlaylist2)
                }
            assertThat(response.status).isEqualTo(HttpStatusCode.NoContent)

            // Read Playlist directly from database & verify it
            val playlist = transaction { PlaylistEntity[playlistId] }.toModel()
            assertThat(playlist.id).isEqualTo(playlistId)
            assertThat(playlist.ownerId).isEqualTo(testUserId)
            assertThat(playlist.name).isEqualTo(testPlaylist2.name)
        }

    @Test
    fun testDeletePlaylist() =
        runBlocking {
            // Add Playlist directly into database
            val playlistId = addPLaylistToDatabase(ownerId = testUserId).id!!

            // delete it
            val response =
                client.delete("v1/playlists/$playlistId") {
                    bearerAuth(testUserToken)
                }
            assertThat(response.status).isEqualTo(HttpStatusCode.NoContent)

            // make sure doesn't exist in database
            val exists = transaction { PlaylistEntity.existsHavingId(playlistId) }
            assertThat(exists).isFalse()
        }

    @Test
    fun testPutPlaylistSong() =
        runBlocking {
            // Add Playlist directly into database
            val playlistId = addPLaylistToDatabase(ownerId = testUserId).id!!

            // Add Song directly into database
            val songId = addSongToDatabase(ownerId = testUserId).id!!

            // Put song into playlist
            val response =
                client.put("v1/playlists/$playlistId/songs") {
                    bearerAuth(testUserToken)
                    contentType(ContentType.Application.Json)
                    setBody(SongIdBody(songId))
                }
            assertThat(response.status).isEqualTo(HttpStatusCode.NoContent)

            // Verify directly from database
            val exists =
                transaction {
                    SongsInPlaylistsTable.exists {
                        (SongsInPlaylistsTable.playlistId eq playlistId) and (SongsInPlaylistsTable.songId eq songId)
                    }
                }
            assertThat(exists).isTrue()
        }

    @Test
    fun testDeletePlaylistSong() =
        runBlocking {
            // Add Playlist directly into database
            val playlistId = addPLaylistToDatabase(ownerId = testUserId).id!!

            // Add Song directly into database
            val songId = addSongToDatabase(ownerId = testUserId).id!!

            // Add song to playlist directly into database
            transaction {
                SongsInPlaylistsTable.insert {
                    it[this.songId] = songId
                    it[this.playlistId] = playlistId
                }
            }

            // delete it
            val response =
                client.delete("v1/playlists/$playlistId/songs/$songId") {
                    bearerAuth(testUserToken)
                }
            assertThat(response.status).isEqualTo(HttpStatusCode.NoContent)

            // Verify that is gone directly from database
            val exists =
                transaction {
                    SongsInPlaylistsTable.exists {
                        (SongsInPlaylistsTable.playlistId eq playlistId) and (SongsInPlaylistsTable.songId eq songId)
                    }
                }
            assertThat(exists).isFalse()
        }

    @Test
    fun testGetPlaylistSongs() =
        runBlocking {
            // Add Playlist directly into database
            val playlistId = addPLaylistToDatabase(ownerId = testUserId).id!!

            // Create songs directly into database
            val expectedSongs = mutableListOf<Song>()
            for (offset in 0..30) {
                expectedSongs += addSongToDatabase(offset, testUserId)
            }

            // add songs to playlist directly into database
            transaction {
                for (song in expectedSongs) {
                    SongsInPlaylistsTable.insert {
                        it[this.songId] = song.id!!
                        it[this.playlistId] = playlistId
                    }
                }
            }

            // Get playlist songs forcing pagination
            var offset = 0
            val limit = 5
            val actualSongs = mutableListOf<Song>()
            while (true) {
                val response =
                    client.get("v1/playlists/$playlistId/songs") {
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
    fun testGetPlaylistCount() =
        runBlocking {
            var count = 0L
            while (true) {
                val response =
                    client.get("v1/playlists/count") {
                        bearerAuth(testUserToken)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val actualCount = response.body<CountResponse>().count
                assertThat(actualCount).isEqualTo(count)

                if (++count == 10L) break

                addPLaylistToDatabase(ownerId = testUserId, offset = count.toInt())
            }
        }
}

private fun addPLaylistToDatabase(
    offset: Int = 0,
    ownerId: UserId? = null
): Playlist {
    val ownerEntityId =
        ownerId?.let {
            EntityID(it, UserTable)
        } ?: transaction {
            UserEntity.new {
                email = "artist$offset@newm.io"
            }
        }.id
    return transaction {
        PlaylistEntity.new {
            this.ownerId = ownerEntityId
            name = "playlist$offset"
        }
    }.toModel()
}
