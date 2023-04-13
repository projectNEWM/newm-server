package io.newm.server.features.distribution

import io.newm.server.features.distribution.model.GetGenresResponse
import io.newm.server.features.song.database.SongEntity

/**
 * Higher level api for working with a music distribution service
 */
interface DistributionRepository {
    suspend fun getGenres(): GetGenresResponse

    suspend fun distributeSong(song: SongEntity)

    // TODO: Add other distribution repository things
}
