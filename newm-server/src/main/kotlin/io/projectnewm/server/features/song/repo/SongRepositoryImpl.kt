package io.projectnewm.server.features.song.repo

import io.projectnewm.server.features.song.model.Song
import kotlinx.coroutines.delay

internal class SongRepositoryImpl : SongRepository {

    override suspend fun getSongs(): List<Song> {
        delay(1000)
        return mockSongs
    }
}
