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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.koin.core.parameter.parametersOf

class SoundCloudProfileUrlVerifier(
    private val httpClient: HttpClient,
) : OutletProfileUrlVerifier {
    private val logger: Logger by inject { parametersOf(javaClass.simpleName) }

    override suspend fun verify(outletProfileUrl: String, stageOrFullName: String) {
        val doc = withContext(Dispatchers.IO) {
            try {
                Jsoup.connect(outletProfileUrl).get()
            } catch (e: HttpStatusException) {
                throw OutletProfileUrlVerificationException("SoundCloud profile not found for $outletProfileUrl")
            }
        }
        val userId = doc.select("meta[property='twitter:app:url:iphone']").attr("content").substringAfterLast(':')
        val response = httpClient.get(
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
