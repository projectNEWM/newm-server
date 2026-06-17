package io.newm.server.features.playlist.database

import io.newm.server.features.song.database.SongTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import java.util.UUID

object SongsInPlaylistsTable : Table(name = "songs_in_playlists") {
    val songId: Column<EntityID<UUID>> =
        reference("song_id", SongTable, onUpdate = ReferenceOption.NO_ACTION, onDelete = ReferenceOption.NO_ACTION)
    val playlistId: Column<EntityID<UUID>> =
        reference("playlist_id", PlaylistTable, onUpdate = ReferenceOption.NO_ACTION, onDelete = ReferenceOption.NO_ACTION)

    override val primaryKey = PrimaryKey(songId, playlistId)
}
