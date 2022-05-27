package io.projectnewm.server.features.playlist

import com.google.common.truth.Truth.assertThat
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.projectnewm.server.BaseApplicationTests
import io.projectnewm.server.ext.exists
import io.projectnewm.server.features.playlist.database.PlaylistEntity
import io.projectnewm.server.features.playlist.database.PlaylistTable
import io.projectnewm.server.features.playlist.database.SongsInPlaylistsTable
import io.projectnewm.server.features.playlist.model.Playlist
import io.projectnewm.server.features.playlist.model.PlaylistIdBody
import io.projectnewm.server.features.song.database.SongEntity
import io.projectnewm.server.features.song.database.SongTable
import io.projectnewm.server.features.song.model.Song
import io.projectnewm.server.features.song.model.SongIdBody
import io.projectnewm.server.features.song.testSong1
import io.projectnewm.server.features.song.testSongs
import io.projectnewm.server.features.user.database.UserTable
import java.time.LocalDateTime
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PlaylistRoutesTests : BaseApplicationTests() {

    @BeforeEach
    fun beforeEach() {
        transaction {
            PlaylistTable.deleteAll()
            SongTable.deleteAll()
        }
    }

    @Test
    fun testPostPlaylist() = runBlocking {
        val startTime = LocalDateTime.now()

        // Post
        val response = client.post("v1/playlists") {
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
    fun testGetPlaylist() = runBlocking {
        // Add Playlist directly into database
        val playlist = transaction {
            PlaylistEntity.new {
                ownerId = EntityID(testUserId, UserTable)
                name = testPlaylist1.name!!
            }
        }.toModel()

        // Get it
        val response = client.get("v1/playlists/${playlist.id}") {
            bearerAuth(testUserToken)
            accept(ContentType.Application.Json)
        }
        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        assertThat(response.body<Playlist>()).isEqualTo(playlist)
    }

    @Test
    fun testGetPlaylists() = runBlocking {
        // Add Playlists directly into database
        val playlists = mutableListOf<Playlist>()
        for (playlist in testPlaylists) {
            playlists += transaction {
                PlaylistEntity.new {
                    ownerId = EntityID(testUserId, UserTable)
                    name = playlist.name!!
                }
            }.toModel()
        }

        // Get all playlists & verify
        val response = client.get("v1/playlists") {
            bearerAuth(testUserToken)
            accept(ContentType.Application.Json)
        }
        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        assertThat(response.body<List<Playlist>>()).isEqualTo(playlists)
    }

    @Test
    fun testPatchPlaylist() = runBlocking {
        // Add Playlist1 directly into database
        val playlistId = transaction {
            PlaylistEntity.new {
                ownerId = EntityID(testUserId, UserTable)
                name = testPlaylist1.name!!
            }
        }.id.value

        // Patch it with Playlist2
        val response = client.patch("v1/playlists/$playlistId") {
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
    fun testDeletePlaylist() = runBlocking {
        // Add playlist directly into database
        val playlistId = transaction {
            PlaylistEntity.new {
                ownerId = EntityID(testUserId, UserTable)
                name = testPlaylist1.name!!
            }
        }.id.value

        // delete it
        val response = client.delete("v1/playlists/$playlistId") {
            bearerAuth(testUserToken)
        }
        assertThat(response.status).isEqualTo(HttpStatusCode.NoContent)

        // make sure doesn't exist in database
        val exists = transaction { PlaylistEntity.exists(playlistId) }
        assertThat(exists).isFalse()
    }

    @Test
    fun testPutPlaylistSong() = runBlocking {
        // Add playlist directly into database
        val playlistId = transaction {
            PlaylistEntity.new {
                ownerId = EntityID(testUserId, UserTable)
                name = testPlaylist1.name!!
            }
        }.id.value

        // Add Song directly into database
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

        // Put song into playlist
        val response = client.put("v1/playlists/$playlistId/songs") {
            bearerAuth(testUserToken)
            contentType(ContentType.Application.Json)
            setBody(SongIdBody(songId))
        }
        assertThat(response.status).isEqualTo(HttpStatusCode.NoContent)

        // Verify directly from database
        val exists = transaction {
            SongsInPlaylistsTable.exists {
                (SongsInPlaylistsTable.playlistId eq playlistId) and (SongsInPlaylistsTable.songId eq songId)
            }
        }
        assertThat(exists).isTrue()
    }

    @Test
    fun testDeletePlaylistSong() = runBlocking {
        // Add playlist directly into database
        val playlistId = transaction {
            PlaylistEntity.new {
                ownerId = EntityID(testUserId, UserTable)
                name = testPlaylist1.name!!
            }
        }.id.value

        // Add Song directly into database
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

        // Add song to playlist directly into database
        transaction {
            SongsInPlaylistsTable.insert {
                it[this.songId] = songId
                it[this.playlistId] = playlistId
            }
        }

        // delete it
        val response = client.delete("v1/playlists/$playlistId/songs/$songId") {
            bearerAuth(testUserToken)
        }
        assertThat(response.status).isEqualTo(HttpStatusCode.NoContent)

        // Verify that is gone directly from database
        val exists = transaction {
            SongsInPlaylistsTable.exists {
                (SongsInPlaylistsTable.playlistId eq playlistId) and (SongsInPlaylistsTable.songId eq songId)
            }
        }
        assertThat(exists).isFalse()
    }

    @Test
    fun testGetPlaylistSongs() = runBlocking {
        // Create playlist directly into database
        val playlistId = transaction {
            PlaylistEntity.new {
                ownerId = EntityID(testUserId, UserTable)
                name = testPlaylist1.name!!
            }
        }.id.value

        // Create songs directly into database
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

        // add songs to playlist directly into database
        transaction {
            for (song in songs) {
                SongsInPlaylistsTable.insert {
                    it[this.songId] = song.id!!
                    it[this.playlistId] = playlistId
                }
            }
        }

        // Get playlist songs and verify
        val response = client.get("v1/playlists/$playlistId/songs") {
            bearerAuth(testUserToken)
            accept(ContentType.Application.Json)
        }
        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        assertThat(response.body<List<Song>>()).isEqualTo(songs)
    }
}
