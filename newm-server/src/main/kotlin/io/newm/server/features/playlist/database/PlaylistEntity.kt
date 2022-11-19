package io.newm.server.features.playlist.database

import io.newm.server.features.playlist.model.Playlist
import io.newm.server.features.song.database.SongEntity
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import java.util.*

class PlaylistEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    val createdAt by PlaylistTable.createdAt
    var ownerId by PlaylistTable.ownerId
    var name by PlaylistTable.name
    val songs by SongEntity via SongsInPlaylistsTable

    fun addSong(songId: UUID) {
        SongsInPlaylistsTable.insert {
            it[this.songId] = songId
            it[this.playlistId] = id
        }
    }

    fun deleteSong(songId: UUID) {
        SongsInPlaylistsTable.deleteWhere {
            (this.songId eq songId) and (playlistId eq id)
        }
    }

    fun toModel(): Playlist = Playlist(
        id = id.value,
        createdAt = createdAt,
        ownerId = ownerId.value,
        name = name
    )

    companion object : UUIDEntityClass<PlaylistEntity>(PlaylistTable) {
        fun allByOwnerId(ownerId: UUID): SizedIterable<PlaylistEntity> = PlaylistEntity.find {
            PlaylistTable.ownerId eq ownerId
        }
    }
}
