package io.newm.server.features.user.verify

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import java.net.URI

class SoundCloudProfileUrlVerifier(
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
            throw OutletProfileUrlVerificationException("SoundCloud profile URL must be https")
        }
        if (url.host?.equals("soundcloud.com", true) == false) {
            throw OutletProfileUrlVerificationException("SoundCloud profile URL must be from soundcloud.com")
        }

        val doc =
            withContext(Dispatchers.IO) {
                try {
                    Jsoup.connect(outletProfileUrl).get()
                } catch (e: HttpStatusException) {
                    throw OutletProfileUrlVerificationException("SoundCloud profile not found for $outletProfileUrl")
                }
            }
        val userId = doc.select("meta[property='twitter:app:url:iphone']").attr("content").substringAfterLast(':')
        val response =
            httpClient.get(
                "https://api.soundcloud.com/users/$userId"
            ) {
                accept(ContentType.Application.Json)
            }
        if (!response.status.isSuccess()) {
            throw OutletProfileUrlVerificationException("SoundCloud profile not found for $userId")
        }
        val soundCloudArtistResponse: SoundCloudArtistResponse = response.body()
        logger.info { "SoundCloud profile response for $userId : $soundCloudArtistResponse" }
        // NOTE: Name matching not required for SoundCloud in Eveara
    }

    @Serializable
    private data class SoundCloudArtistResponse(
        @SerialName("username")
        val username: String? = null,
        @SerialName("first_name")
        val firstName: String? = null,
        @SerialName("last_name")
        val lastName: String? = null,
        @SerialName("full_name")
        val fullName: String? = null,
    )
}
