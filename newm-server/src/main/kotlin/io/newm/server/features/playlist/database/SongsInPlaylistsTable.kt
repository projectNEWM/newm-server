package io.newm.server.features.playlist.database

import io.newm.server.features.song.database.SongTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import java.util.UUID

object SongsInPlaylistsTable : Table(name = "songs_in_playlists") {
    val songId: Column<EntityID<UUID>> = reference("song_id", SongTable, onDelete = ReferenceOption.CASCADE)
    val playlistId: Column<EntityID<UUID>> = reference("playlist_id", PlaylistTable, onDelete = ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(songId, playlistId)
}
