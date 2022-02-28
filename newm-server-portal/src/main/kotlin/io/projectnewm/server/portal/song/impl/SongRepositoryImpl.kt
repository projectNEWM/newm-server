package io.projectnewm.server.portal.song.impl

import io.projectnewm.server.portal.model.GetSongsResponse
import io.projectnewm.server.portal.song.SongRepository
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

internal class SongRepositoryImpl : SongRepository {

    override suspend fun getSongs(): GetSongsResponse {
        delay(1000)
        return GetSongsResponse(
            version = 1,
            time = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            songs = mockSongs
        )
    }
}
