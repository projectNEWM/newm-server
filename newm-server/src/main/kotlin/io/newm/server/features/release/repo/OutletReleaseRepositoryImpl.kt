package io.newm.server.features.release.repo

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.plugins.retry
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.newm.server.config.repo.ConfigRepository
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_NEWM_PLAYLIST_ID
import io.newm.server.features.distribution.model.SmartLink
import io.newm.server.features.release.model.SpotifyPlaylistRequest
import io.newm.server.features.release.model.SpotifySearchResponse
import io.newm.server.features.song.database.SongEntity
import io.newm.server.ktx.checkedBody
import io.newm.server.typealiases.SongId
import org.jetbrains.exposed.sql.transactions.transaction

private const val SPOTIFY_SEARCH_API_URL = "https://api.spotify.com/v1/search"
private const val SPOTIFY_PLAYLIST_API_URL = "https://api.spotify.com/v1/playlists/"

internal class OutletReleaseRepositoryImpl(
    private val httpClient: HttpClient,
    private val configRepository: ConfigRepository
) : OutletReleaseRepository {
    private val logger = KotlinLogging.logger {}

    override suspend fun isSongReleased(songId: SongId): Boolean {
        val response = searchSpotifyTracks(songId) ?: return false
        // Call addSongToPlaylist in case the song was released
        if (response.tracks.total > 0) {
            try {
                val httpResponse = addSongToPlaylist(response.tracks.items[0].uri)
                if (httpResponse.status.isSuccess()) {
                    logger.debug { "Song was successfully added to spotify playlist" }
                } else {
                    logger.error { "Song was not added to spotify playlist, $httpResponse" }
                }
            } catch (e: Throwable) {
                logger.error { "Error adding song to spotify playlist, $e" }
            }
        }
        return response.tracks.total > 0
    }

    override suspend fun addSongToPlaylist(trackUri: String): HttpResponse {
        logger.debug { "Adding song with track uri=$trackUri to spotify playlist" }
        val playlistId = configRepository.getString(CONFIG_KEY_NEWM_PLAYLIST_ID)
        val response = httpClient.post("$SPOTIFY_PLAYLIST_API_URL$playlistId/tracks") {
            retry {
                maxRetries = 2
                delayMillis { 500L }
            }
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(
                SpotifyPlaylistRequest(
                    uris = listOf("spotify:track:$trackUri"),
                    position = 0
                )
            )
        }
        return response
    }

    override suspend fun getSmartLinks(songId: SongId): List<SmartLink> =
        searchSpotifyTracks(songId)
            ?.tracks
            ?.items
            ?.firstOrNull()
            ?.externalUrls
            ?.values
            ?.firstOrNull()
            ?.let { listOf(SmartLink("Spotify", it)) }
            .orEmpty()

    private suspend fun searchSpotifyTracks(songId: SongId): SpotifySearchResponse? {
        val rawIsrc = transaction { SongEntity[songId].isrc } ?: return null
        val isrc = rawIsrc
            .replace("-", "")
            .lowercase()
        logger.debug { "searchSpotifyTracks: songId = $songId,  isrc = $isrc" }
        return httpClient
            .get(SPOTIFY_SEARCH_API_URL) {
                parameter("market", "US")
                parameter("limit", "1")
                parameter("type", "track")
                parameter("q", "isrc:$isrc")
                accept(ContentType.Application.Json)
            }.checkedBody<SpotifySearchResponse>()
    }
}
