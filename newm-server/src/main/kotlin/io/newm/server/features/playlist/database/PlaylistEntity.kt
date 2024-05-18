package io.newm.server.features.playlist.database

import io.newm.server.features.playlist.model.Playlist
import io.newm.server.features.playlist.model.PlaylistFilters
import io.newm.server.features.song.database.SongEntity
import io.newm.server.typealiases.SongId
import io.newm.server.typealiases.UserId
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.AndOp
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.SqlExpressionBuilder.notInList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import java.time.LocalDateTime
import java.util.UUID

class PlaylistEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    val createdAt: LocalDateTime by PlaylistTable.createdAt
    var ownerId: EntityID<UserId> by PlaylistTable.ownerId
    var name: String by PlaylistTable.name
    val songs: SizedIterable<SongEntity> by SongEntity via SongsInPlaylistsTable

    fun addSong(songId: SongId) {
        SongsInPlaylistsTable.insert {
            it[this.songId] = songId
            it[this.playlistId] = id
        }
    }

    fun deleteSong(songId: SongId) {
        SongsInPlaylistsTable.deleteWhere {
            (this.songId eq songId) and (playlistId eq id)
        }
    }

    fun toModel(): Playlist =
        Playlist(
            id = id.value,
            createdAt = createdAt,
            ownerId = ownerId.value,
            name = name
        )

    companion object : UUIDEntityClass<PlaylistEntity>(PlaylistTable) {
        fun all(filters: PlaylistFilters): SizedIterable<PlaylistEntity> {
            val ops = mutableListOf<Op<Boolean>>()
            with(filters) {
                olderThan?.let {
                    ops += PlaylistTable.createdAt less it
                }
                newerThan?.let {
                    ops += PlaylistTable.createdAt greater it
                }
                ids?.includes?.let {
                    ops += PlaylistTable.id inList it
                }
                ids?.excludes?.let {
                    ops += PlaylistTable.id notInList it
                }
                ownerIds?.includes?.let {
                    ops += PlaylistTable.ownerId inList it
                }
                ownerIds?.excludes?.let {
                    ops += PlaylistTable.ownerId notInList it
                }
            }
            return (if (ops.isEmpty()) all() else find(AndOp(ops)))
                .orderBy(PlaylistTable.createdAt to (filters.sortOrder ?: SortOrder.ASC))
        }
    }
}
