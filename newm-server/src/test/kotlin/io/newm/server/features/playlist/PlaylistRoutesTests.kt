package io.newm.server.features.playlist

import com.google.common.truth.Truth.assertThat
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.newm.server.BaseApplicationTests
import io.newm.shared.ext.exists
import io.newm.shared.ext.existsHavingId
import io.newm.server.features.playlist.database.PlaylistEntity
import io.newm.server.features.playlist.database.PlaylistTable
import io.newm.server.features.playlist.database.SongsInPlaylistsTable
import io.newm.server.features.playlist.model.Playlist
import io.newm.server.features.playlist.model.PlaylistIdBody
import io.newm.server.features.song.database.SongEntity
import io.newm.server.features.song.database.SongTable
import io.newm.server.features.song.model.MarketplaceStatus
import io.newm.server.features.song.model.MintingStatus
import io.newm.server.features.song.model.Song
import io.newm.server.features.song.model.SongIdBody
import io.newm.server.features.song.testSong1
import io.newm.server.features.user.database.UserEntity
import io.newm.server.features.user.database.UserTable
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

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
    fun testGetAllPlaylists() = runBlocking {
        // Add Users + Playlists directly into database
        val expectedPlaylists = mutableListOf<Playlist>()
        for (offset in 0..30) {
            val ownerId = transaction {
                UserEntity.new {
                    email = "artist$offset@newm.io"
                }
            }.id.value

            expectedPlaylists += transaction {
                PlaylistEntity.new {
                    this.ownerId = EntityID(ownerId, UserTable)
                    name = "name$offset"
                }
            }.toModel()
        }

        // Get all playlists forcing pagination
        var offset = 0
        val limit = 5
        val actualPlaylists = mutableListOf<Playlist>()
        while (true) {
            val response = client.get("v1/playlists") {
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
    fun testGetPlaylistsByIds() = runBlocking {
        // Add Users + Playlists directly into database
        val allPlaylists = mutableListOf<Playlist>()
        for (offset in 0..30) {
            val ownerId = transaction {
                UserEntity.new {
                    email = "artist$offset@newm.io"
                }
            }.id.value

            allPlaylists += transaction {
                PlaylistEntity.new {
                    this.ownerId = EntityID(ownerId, UserTable)
                    name = "name$offset"
                }
            }.toModel()
        }

        // filter out 1st and last
        val expectedPlaylists = allPlaylists.subList(1, allPlaylists.size - 1)
        val ids = expectedPlaylists.map { it.id }.joinToString()

        // Get all playlists forcing pagination
        var offset = 0
        val limit = 5
        val actualPlaylists = mutableListOf<Playlist>()
        while (true) {
            val response = client.get("v1/playlists") {
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
    fun testGetPlaylistsByOwnerIds() = runBlocking {
        // Add Users + Playlists directly into database
        val allPlaylists = mutableListOf<Playlist>()
        for (offset in 0..30) {
            val ownerId = transaction {
                UserEntity.new {
                    email = "artist$offset@newm.io"
                }
            }.id.value

            allPlaylists += transaction {
                PlaylistEntity.new {
                    this.ownerId = EntityID(ownerId, UserTable)
                    name = "name$offset"
                }
            }.toModel()
        }

        // filter out 1st and last
        val expectedPlaylists = allPlaylists.subList(1, allPlaylists.size - 1)
        val ownerIds = expectedPlaylists.map { it.ownerId }.joinToString()

        // Get all playlists forcing pagination
        var offset = 0
        val limit = 5
        val actualPlaylists = mutableListOf<Playlist>()
        while (true) {
            val response = client.get("v1/playlists") {
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
    fun testGetPlaylistsByOlderThan() = runBlocking {
        // Add Users + Playlists directly into database
        val allPlaylists = mutableListOf<Playlist>()
        for (offset in 0..30) {
            val ownerId = transaction {
                UserEntity.new {
                    email = "artist$offset@newm.io"
                }
            }.id.value

            allPlaylists += transaction {
                PlaylistEntity.new {
                    this.ownerId = EntityID(ownerId, UserTable)
                    name = "name$offset"
                }
            }.toModel()
        }

        // filter out newest one
        val expectedPlaylists = allPlaylists.subList(0, allPlaylists.size - 1)
        val olderThan = allPlaylists.last().createdAt

        // Get all playlists forcing pagination
        var offset = 0
        val limit = 5
        val actualPlaylists = mutableListOf<Playlist>()
        while (true) {
            val response = client.get("v1/playlists") {
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
    fun testGetPlaylistsByNewerThan() = runBlocking {
        // Add Users + Playlists directly into database
        val allPlaylists = mutableListOf<Playlist>()
        for (offset in 0..30) {
            val ownerId = transaction {
                UserEntity.new {
                    email = "artist$offset@newm.io"
                }
            }.id.value

            allPlaylists += transaction {
                PlaylistEntity.new {
                    this.ownerId = EntityID(ownerId, UserTable)
                    name = "name$offset"
                }
            }.toModel()
        }

        // filter out newest one
        val expectedPlaylists = allPlaylists.subList(1, allPlaylists.size)
        val newerThan = allPlaylists.first().createdAt

        // Get all playlists forcing pagination
        var offset = 0
        val limit = 5
        val actualPlaylists = mutableListOf<Playlist>()
        while (true) {
            val response = client.get("v1/playlists") {
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
        val exists = transaction { PlaylistEntity.existsHavingId(playlistId) }
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
                genres = testSong1.genres!!.toTypedArray()
                coverArtUrl = testSong1.coverArtUrl
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
                genres = testSong1.genres!!.toTypedArray()
                coverArtUrl = testSong1.coverArtUrl
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
        val expectedSongs = mutableListOf<Song>()
        for (offset in 0..30) {
            expectedSongs += transaction {
                SongEntity.new {
                    ownerId = EntityID(testUserId, UserTable)
                    title = "title$offset"
                    genres = arrayOf("genre${offset}_0", "genre${offset}_1")
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
            val response = client.get("v1/playlists/$playlistId/songs") {
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
}
