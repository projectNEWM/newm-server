package io.newm.server.features.release.repo

import io.newm.server.typealiases.SongId

interface OutletReleaseRepository {
    suspend fun isSongReleased(songId: SongId): Boolean
}
