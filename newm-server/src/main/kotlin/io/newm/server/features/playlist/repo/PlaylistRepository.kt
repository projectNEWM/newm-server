package io.newm.server.features.playlist.repo

import io.newm.server.features.playlist.model.Playlist
import io.newm.server.features.playlist.model.PlaylistFilters
import io.newm.server.features.song.model.Song
import io.newm.server.typealiases.SongId
import io.newm.server.typealiases.UserId
import java.util.UUID

interface PlaylistRepository {
    suspend fun add(
        playlist: Playlist,
        ownerId: UserId
    ): UUID

    suspend fun update(
        playlist: Playlist,
        playlistId: UUID,
        requesterId: UserId
    )

    suspend fun delete(
        playlistId: UUID,
        requesterId: UserId
    )

    suspend fun get(playlistId: UUID): Playlist

    suspend fun getAll(
        filters: PlaylistFilters,
        offset: Int,
        limit: Int
    ): List<Playlist>

    suspend fun getAllCount(filters: PlaylistFilters): Long

    suspend fun addSong(
        playlistId: UUID,
        songId: SongId,
        requesterId: UserId
    )

    suspend fun deleteSong(
        playlistId: UUID,
        songId: SongId,
        requesterId: UserId
    )

    suspend fun getSongs(
        playlistId: UUID,
        offset: Int,
        limit: Int
    ): List<Song>
}
