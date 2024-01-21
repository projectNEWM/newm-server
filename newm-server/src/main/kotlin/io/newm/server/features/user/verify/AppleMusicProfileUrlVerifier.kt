package io.newm.server.features.user.verify

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.isSuccess
import io.ktor.util.logging.Logger
import io.newm.shared.koin.inject
import io.newm.shared.ktx.info
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.core.parameter.parametersOf

class AppleMusicProfileUrlVerifier(
    private val httpClient: HttpClient,
) : OutletProfileUrlVerifier {
    private val logger: Logger by inject { parametersOf(javaClass.simpleName) }

    override suspend fun verify(outletProfileUrl: String, stageOrFullName: String) {
        val appleProfileId = outletProfileUrl.substringAfterLast("/").substringBefore("?")
        val response = httpClient.get(
            "https://api.music.apple.com/v1/catalog/us/artists/$appleProfileId"
        ) {
            accept(ContentType.Application.Json)
        }
        if (!response.status.isSuccess()) {
            throw IllegalArgumentException("Apple Music profile not found for $appleProfileId")
        }
        val appleMusicArtistResponse: AppleMusicArtistResponse = response.body()
        if (appleMusicArtistResponse.data.isEmpty()) {
            throw IllegalArgumentException("Apple Music profile not found for $appleProfileId")
        }
        logger.info { "Apple Music profile response for $appleProfileId : $appleMusicArtistResponse" }
        if (appleMusicArtistResponse.data[0].attributes.name != stageOrFullName) {
            throw IllegalArgumentException("Apple Music profile name (${appleMusicArtistResponse.data[0].attributes.name}) does not match stageOrFullName ($stageOrFullName) for $appleProfileId")
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
