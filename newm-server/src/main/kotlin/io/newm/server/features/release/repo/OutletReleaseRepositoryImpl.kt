package io.newm.server.features.release.repo

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.newm.server.features.release.model.SpotifySearchResponse
import io.newm.server.features.song.repo.SongRepository
import io.newm.server.ktx.checkedBody
import io.newm.shared.koin.inject
import io.newm.shared.ktx.debug
import org.koin.core.parameter.parametersOf
import org.slf4j.Logger
import java.util.UUID

private const val SPOTIFY_SEARCH_API_URL = "https://api.spotify.com/v1/search"

internal class OutletReleaseRepositoryImpl(
    private val httpClient: HttpClient,
    private val songRepository: SongRepository
) : OutletReleaseRepository {
    private val logger: Logger by inject { parametersOf(javaClass.simpleName) }

    override suspend fun isSongReleased(songId: UUID): Boolean {
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
