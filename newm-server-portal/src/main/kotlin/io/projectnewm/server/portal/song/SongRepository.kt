package io.projectnewm.server.portal.song

import io.projectnewm.server.portal.model.GetSongsResponse

interface SongRepository {
    suspend fun getSongs(): GetSongsResponse
}
