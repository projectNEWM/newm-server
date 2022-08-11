package io.newm.server.features.playlist.repo

import io.newm.server.features.playlist.model.Playlist
import io.newm.server.features.song.model.Song
import java.util.UUID

interface PlaylistRepository {
    suspend fun add(playlist: Playlist, ownerId: UUID): UUID
    suspend fun update(playlist: Playlist, playlistId: UUID, requesterId: UUID)
    suspend fun delete(playlistId: UUID, requesterId: UUID)
    suspend fun get(playlistId: UUID): Playlist
    suspend fun getAllByOwnerId(ownerId: UUID): List<Playlist>
    suspend fun addSong(playlistId: UUID, songId: UUID, requesterId: UUID)
    suspend fun deleteSong(playlistId: UUID, songId: UUID, requesterId: UUID)
    suspend fun getSongs(playlistId: UUID): List<Song>
}
