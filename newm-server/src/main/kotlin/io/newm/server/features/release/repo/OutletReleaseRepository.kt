package io.newm.server.features.release.repo

import io.ktor.client.statement.*
import io.newm.server.typealiases.SongId

interface OutletReleaseRepository {
    suspend fun isSongReleased(songId: SongId): Boolean

    suspend fun addSongToPlaylist(trackUri: String): HttpResponse
}
