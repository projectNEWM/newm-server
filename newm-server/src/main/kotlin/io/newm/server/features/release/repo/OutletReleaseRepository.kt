package io.newm.server.features.release.repo

import java.util.UUID

interface OutletReleaseRepository {
    suspend fun isSongReleased(songId: UUID): Boolean
}
