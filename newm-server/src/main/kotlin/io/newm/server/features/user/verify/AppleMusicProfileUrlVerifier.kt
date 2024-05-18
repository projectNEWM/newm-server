package io.newm.server.features.user.verify

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.net.URI

class AppleMusicProfileUrlVerifier(
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
            throw OutletProfileUrlVerificationException("Apple Music profile URL must be https")
        }
        if (url.host?.equals("music.apple.com", true) == false) {
            throw OutletProfileUrlVerificationException("Apple Music profile URL must be from music.apple.com")
        }

        val appleProfileId = outletProfileUrl.substringAfterLast("/").substringBefore("?")
        val response =
            httpClient.get(
                "https://api.music.apple.com/v1/catalog/us/artists/$appleProfileId"
            ) {
                accept(ContentType.Application.Json)
            }
        if (!response.status.isSuccess()) {
            throw OutletProfileUrlVerificationException("Apple Music profile not found for $appleProfileId")
        }
        val appleMusicArtistResponse: AppleMusicArtistResponse = response.body()
        if (appleMusicArtistResponse.data.isEmpty()) {
            throw OutletProfileUrlVerificationException("Apple Music profile not found for $appleProfileId")
        }
        logger.info { "Apple Music profile response for $appleProfileId : $appleMusicArtistResponse" }
        if (appleMusicArtistResponse.data[0].attributes.name != stageOrFullName) {
            throw OutletProfileUrlVerificationException(
                "Apple Music profile name (${appleMusicArtistResponse.data[0].attributes.name}) does not match stageOrFullName ($stageOrFullName) for $appleProfileId"
            )
        }
    }

    @Serializable
    private data class AppleMusicArtistResponse(
        @SerialName("data")
        val data: List<AppleMusicArtist>,
    )

    @Serializable
    private data class AppleMusicArtist(
        @SerialName("id")
        val id: String,
        @SerialName("attributes")
        val attributes: AppleMusicArtistAttributes,
    )

    @Serializable
    private data class AppleMusicArtistAttributes(
        @SerialName("name")
        val name: String,
    )
}
