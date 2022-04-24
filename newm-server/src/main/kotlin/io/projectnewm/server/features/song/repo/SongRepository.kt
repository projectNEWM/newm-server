package io.projectnewm.server.features.song.repo

import io.projectnewm.server.features.song.model.Song
import java.util.UUID

interface SongRepository {
    suspend fun add(song: Song, ownerId: UUID)
    suspend fun update(song: Song, songId: UUID, requesterId: UUID)
    suspend fun delete(songId: UUID, requesterId: UUID)
    suspend fun get(songId: UUID): Song
    suspend fun getAllByOwnerId(ownerId: UUID): List<Song>
}
