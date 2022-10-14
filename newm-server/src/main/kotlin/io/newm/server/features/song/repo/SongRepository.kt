package io.newm.server.features.song.repo

import io.newm.server.features.song.model.Song
import io.newm.server.features.song.model.SongFilter
import java.util.UUID

interface SongRepository {
    suspend fun add(song: Song, ownerId: UUID): UUID
    suspend fun update(song: Song, songId: UUID, requesterId: UUID)
    suspend fun delete(songId: UUID, requesterId: UUID)
    suspend fun get(songId: UUID): Song
    suspend fun getAll(filter: SongFilter, offset: Int, limit: Int): List<Song>
    suspend fun generateUploadUrl(songId: UUID, requesterId: UUID, fileName: String): String
    suspend fun updateStreamUrl(songId: UUID, streamUrl: String)
}
