package io.projectnewm.server.features.song.repo

import io.projectnewm.server.features.song.model.Song

interface SongRepository {
    suspend fun getSongs(): List<Song>
}
