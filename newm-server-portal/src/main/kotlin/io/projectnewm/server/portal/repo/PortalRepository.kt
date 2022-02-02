package io.projectnewm.server.portal.repo

import io.projectnewm.server.portal.model.GetSongsResponse
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class PortalRepository {

    suspend fun getSongs(): GetSongsResponse {
        delay(1000)
        return GetSongsResponse(
            version = 1,
            time = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            songs = mockSongs
        )
    }
}
