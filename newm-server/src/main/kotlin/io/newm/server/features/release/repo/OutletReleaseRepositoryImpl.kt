package io.newm.server.features.release.repo

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.newm.server.features.release.model.SpotifySearchResponse
import io.newm.server.features.song.repo.SongRepository
import io.newm.server.ktx.checkedBody
import io.newm.server.typealiases.SongId

private const val SPOTIFY_SEARCH_API_URL = "https://api.spotify.com/v1/search"

internal class OutletReleaseRepositoryImpl(
    private val httpClient: HttpClient,
    private val songRepository: SongRepository
) : OutletReleaseRepository {
    private val logger = KotlinLogging.logger {}

    override suspend fun isSongReleased(songId: SongId): Boolean {
        val isrc = songRepository.get(songId).isrc!!.lowercase().replace("-", "")
        logger.debug { "isSongReleased: songId = $songId, isrc = $isrc" }

        val response =
            httpClient.get(SPOTIFY_SEARCH_API_URL) {
                url {
                    with(parameters) {
                        append("market", "US")
                        append("limit", "1")
                        append("type", "track")
                        append("q", "isrc:$isrc")
                    }
                }
                accept(ContentType.Application.Json)
            }.checkedBody<SpotifySearchResponse>()
        return response.tracks.total > 0
    }
}
