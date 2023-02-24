package io.newm.server.features.song.repo

import io.newm.server.features.song.model.Song
import io.newm.server.features.song.model.SongFilters
import io.newm.server.features.song.model.UploadType
import java.util.UUID

interface SongRepository {
    suspend fun add(song: Song, ownerId: UUID): UUID
    suspend fun update(songId: UUID, song: Song, requesterId: UUID? = null)
    suspend fun delete(songId: UUID, requesterId: UUID)
    suspend fun get(songId: UUID): Song
    suspend fun getAll(filters: SongFilters, offset: Int, limit: Int): List<Song>
    suspend fun getGenres(filters: SongFilters, offset: Int, limit: Int): List<String>
    suspend fun generateUploadUrl(uploadType: UploadType, songId: UUID, requesterId: UUID, fileName: String): String
}
