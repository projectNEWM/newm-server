package io.newm.server.features.user.verify

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.net.URI

class SpotifyProfileUrlVerifier(
    private val httpClient: HttpClient,
) : OutletProfileUrlVerifier {
    private val logger = KotlinLogging.logger {}

    override suspend fun verify(
        outletProfileUrl: String,
        stageOrFullName: String
    ) {
        // URL Checks
        val url = URI.create(outletProfileUrl).toURL()
        if (url.protocol != "https") {
            throw OutletProfileUrlVerificationException("Spotify profile URL must be https")
        }
        if (url.host?.equals("open.spotify.com", true) == false) {
            throw OutletProfileUrlVerificationException("Spotify profile URL must be from open.spotify.com")
        }

        val spotifyProfileId = outletProfileUrl.substringAfterLast("/").substringBefore("?")
        val response =
            httpClient.get(
                "https://api.spotify.com/v1/artists/$spotifyProfileId"
            ) {
                accept(ContentType.Application.Json)
            }
        if (!response.status.isSuccess()) {
            throw OutletProfileUrlVerificationException("Spotify profile not found for $spotifyProfileId")
        }
        val spotifyArtistResponse: SpotifyArtistResponse = response.body()
        logger.info { "Spotify profile response for $spotifyProfileId : $spotifyArtistResponse" }
        if (spotifyArtistResponse.name != stageOrFullName) {
            throw OutletProfileUrlVerificationException(
                "Spotify profile name (${spotifyArtistResponse.name}) does not match stageOrFullName ($stageOrFullName) for $spotifyProfileId"
            )
        }
    }

    @Serializable
    private data class SpotifyArtistResponse(
        @SerialName("id")
        val id: String,
        @SerialName("name")
        val name: String,
    )
}
