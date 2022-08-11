package io.newm.server.features.playlist.database

import io.newm.server.features.song.database.SongTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object SongsInPlaylistsTable : Table(name = "songs_in_playlists") {
    val songId = reference("song_id", SongTable, onDelete = ReferenceOption.CASCADE)
    val playlistId = reference("playlist_id", PlaylistTable, onDelete = ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(songId, playlistId)
}
