package io.newm.server.features.release.repo

import io.ktor.client.statement.HttpResponse
import io.newm.server.features.distribution.model.SmartLink
import io.newm.server.typealiases.SongId

interface OutletReleaseRepository {
    suspend fun isSongReleased(songId: SongId): Boolean

    suspend fun addSongToPlaylist(trackUri: String): HttpResponse

    suspend fun getSmartLinks(songId: SongId): List<SmartLink>
}
