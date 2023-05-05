package io.newm.server.features.distribution.eveara

import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.forms.InputProvider
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.utils.io.streams.asInput
import io.newm.server.config.repo.ConfigRepository
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_EVEARA_CLIENT_ID
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_EVEARA_CLIENT_SECRET
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_EVEARA_PARTNER_SUBSCRIPTION_ID
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_EVEARA_SERVER
import io.newm.server.features.collaboration.model.CollaborationFilters
import io.newm.server.features.collaboration.model.CollaborationStatus
import io.newm.server.features.collaboration.repo.CollaborationRepository
import io.newm.server.features.distribution.DistributionRepository
import io.newm.server.features.distribution.model.AddArtistRequest
import io.newm.server.features.distribution.model.AddArtistResponse
import io.newm.server.features.distribution.model.AddParticipantPaypalRequest
import io.newm.server.features.distribution.model.AddParticipantPaypalResponse
import io.newm.server.features.distribution.model.AddParticipantRequest
import io.newm.server.features.distribution.model.AddParticipantResponse
import io.newm.server.features.distribution.model.AddTrackResponse
import io.newm.server.features.distribution.model.AddUserLabelRequest
import io.newm.server.features.distribution.model.AddUserLabelResponse
import io.newm.server.features.distribution.model.AddUserRequest
import io.newm.server.features.distribution.model.AddUserResponse
import io.newm.server.features.distribution.model.AddUserSubscriptionRequest
import io.newm.server.features.distribution.model.AddUserSubscriptionResponse
import io.newm.server.features.distribution.model.DeleteUserLabelResponse
import io.newm.server.features.distribution.model.GetArtistResponse
import io.newm.server.features.distribution.model.GetCountriesResponse
import io.newm.server.features.distribution.model.GetGenresResponse
import io.newm.server.features.distribution.model.GetLanguagesResponse
import io.newm.server.features.distribution.model.GetOutletsResponse
import io.newm.server.features.distribution.model.GetRolesResponse
import io.newm.server.features.distribution.model.GetUserResponse
import io.newm.server.features.distribution.model.GetUserSubscriptionResponse
import io.newm.server.features.distribution.model.Subscription
import io.newm.server.features.distribution.model.UpdateTrackRequest
import io.newm.server.features.distribution.model.UpdateTrackResponse
import io.newm.server.features.distribution.model.UpdateUserLabelResponse
import io.newm.server.features.song.database.SongEntity
import io.newm.server.features.song.model.Song
import io.newm.server.features.user.database.UserEntity
import io.newm.server.features.user.model.User
import io.newm.server.ktx.getSecureConfigString
import io.newm.shared.koin.inject
import io.newm.shared.ktx.getConfigString
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.core.parameter.parametersOf
import org.slf4j.Logger
import java.io.File
import java.time.Duration
import java.util.UUID

class EvearaDistributionRepositoryImpl(
    private val collabRepository: CollaborationRepository,
    private val configRepository: ConfigRepository,
) : DistributionRepository {
    private val log: Logger by inject { parametersOf(javaClass.simpleName) }
    private val httpClient: HttpClient by inject()
    private val applicationEnvironment: ApplicationEnvironment by inject()
    private val evearaServer by lazy { applicationEnvironment.getConfigString(CONFIG_KEY_EVEARA_SERVER) }
    private val evearaApiBaseUrl by lazy { "https://$evearaServer/api/v2.0" }
    private suspend fun evearaClientId() =
        applicationEnvironment.getSecureConfigString(CONFIG_KEY_EVEARA_CLIENT_ID)

    private suspend fun evearaClientSecret() =
        applicationEnvironment.getSecureConfigString(CONFIG_KEY_EVEARA_CLIENT_SECRET)

    private val getTokenMutex = Mutex()
    private val apiTokenCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofHours(1L))
        .build<Int, ApiToken>()

    private val rolesMutex = Mutex()
    private val rolesCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofDays(1L))
        .build<Int, GetRolesResponse>()

    private val genresMutex = Mutex()
    private val genresCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofDays(1L))
        .build<Int, GetGenresResponse>()

    private val languagesMutex = Mutex()
    private val languagesCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofDays(1L))
        .build<Int, GetLanguagesResponse>()

    private val countriesMutex = Mutex()
    private val countriesCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofDays(1L))
        .build<Int, GetCountriesResponse>()

    private val outletsMutex = Mutex()
    private val outletsCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofDays(1L))
        .build<Int, GetOutletsResponse>()

    private suspend fun getEvearaApiToken(): String {
        getTokenMutex.withLock {
            return apiTokenCache.getIfPresent(-1)?.let { apiToken ->
                if (System.currentTimeMillis() > apiToken.expiryTimestampMillis - FIVE_MINUTES_IN_MILLIS) {
                    // Get an access token since it's close to expiry
                    null
                } else {
                    apiToken.accessToken
                }
            } ?: run {
                val response = httpClient.post("$evearaApiBaseUrl/oauth/gettoken") {
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    setBody(
                        EvearaApiGetTokenRequest(
                            grantType = GRANT_TYPE,
                            clientId = evearaClientId(),
                            clientSecret = evearaClientSecret(),
                        )
                    )
                }
                if (!response.status.isSuccess()) {
                    throw ServerResponseException(
                        response,
                        "Error getting Eveara accessToken! - ${response.bodyAsText()}"
                    )
                }
                val apiTokenResponse: EvearaApiTokenResponse = response.body()
                if (!apiTokenResponse.success) {
                    throw ServerResponseException(response, "Error getting Eveara accessToken! success==false")
                }
                val apiToken = ApiToken(
                    accessToken = apiTokenResponse.accessToken,
                    refreshToken = apiTokenResponse.refreshToken,
                    expiryTimestampMillis = System.currentTimeMillis() + apiTokenResponse.expiresInSeconds * 1000L
                )
                apiTokenCache.put(-1, apiToken)

                apiToken.accessToken
            }
        }
    }

    override suspend fun getGenres(): GetGenresResponse = genresMutex.withLock {
        genresCache.getIfPresent(-1) ?: run {
            val response = httpClient.get("$evearaApiBaseUrl/genres") {
                accept(ContentType.Application.Json)
                bearerAuth(getEvearaApiToken())
            }
            if (!response.status.isSuccess()) {
                throw ServerResponseException(response, "Error getting genres!")
            }
            val getGenresResponse: GetGenresResponse = response.body()
            if (!getGenresResponse.success) {
                throw ServerResponseException(response, "Error getting genres! success==false")
            }

            getGenresResponse.also { genresCache.put(-1, it) }
        }
    }

    override suspend fun getRoles(): GetRolesResponse = rolesMutex.withLock {
        rolesCache.getIfPresent(-1) ?: run {
            val response = httpClient.get("$evearaApiBaseUrl/roles") {
                accept(ContentType.Application.Json)
                bearerAuth(getEvearaApiToken())
            }
            if (!response.status.isSuccess()) {
                throw ServerResponseException(response, "Error getting roles!")
            }
            val getRolesResponse: GetRolesResponse = response.body()
            if (!getRolesResponse.success) {
                throw ServerResponseException(response, "Error getting roles! success==false")
            }

            getRolesResponse.also { rolesCache.put(-1, it) }
        }
    }

    override suspend fun getLanguages(): GetLanguagesResponse = languagesMutex.withLock {
        languagesCache.getIfPresent(-1) ?: run {
            val response = httpClient.get("$evearaApiBaseUrl/languages") {
                accept(ContentType.Application.Json)
                bearerAuth(getEvearaApiToken())
            }
            if (!response.status.isSuccess()) {
                throw ServerResponseException(response, "Error getting languages!")
            }
            val getLanguagesResponse: GetLanguagesResponse = response.body()
            if (!getLanguagesResponse.success) {
                throw ServerResponseException(response, "Error getting languages! success==false")
            }

            getLanguagesResponse.also { languagesCache.put(-1, it) }
        }
    }

    override suspend fun getCountries(): GetCountriesResponse = countriesMutex.withLock {
        countriesCache.getIfPresent(-1) ?: run {
            val response = httpClient.get("$evearaApiBaseUrl/countries") {
                accept(ContentType.Application.Json)
                bearerAuth(getEvearaApiToken())
            }
            if (!response.status.isSuccess()) {
                throw ServerResponseException(response, "Error getting countries!")
            }
            val getCountriesResponse: GetCountriesResponse = response.body()
            if (!getCountriesResponse.success) {
                throw ServerResponseException(response, "Error getting countries! success==false")
            }

            getCountriesResponse.also { countriesCache.put(-1, it) }
        }
    }

    override suspend fun getOutlets(): GetOutletsResponse = outletsMutex.withLock {
        outletsCache.getIfPresent(-1) ?: run {
            val response = httpClient.get("$evearaApiBaseUrl/outlets") {
                accept(ContentType.Application.Json)
                bearerAuth(getEvearaApiToken())
            }
            if (!response.status.isSuccess()) {
                throw ServerResponseException(response, "Error getting outlets!")
            }
            val getOutletsResponse: GetOutletsResponse = response.body()
            if (!getOutletsResponse.success) {
                throw ServerResponseException(response, "Error getting outlets! success==false")
            }

            getOutletsResponse.also { outletsCache.put(-1, it) }
        }
    }

    override suspend fun addUser(user: User): AddUserResponse {
        requireNotNull(user.firstName) { "User.firstName must not be null!" }
        requireNotNull(user.lastName) { "User.lastName must not be null!" }
        requireNotNull(user.email) { "User.email must not be null!" }
        val response = httpClient.post("$evearaApiBaseUrl/users") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(getEvearaApiToken())
            setBody(
                AddUserRequest(
                    firstName = user.firstName,
                    lastName = user.lastName,
                    email = user.email,
                )
            )
        }
        if (!response.status.isSuccess()) {
            throw ServerResponseException(response, "Error adding user!")
        }
        val addUserResponse: AddUserResponse = response.body()
        if (!addUserResponse.success) {
            throw ServerResponseException(response, "Error adding user! success==false")
        }

        return addUserResponse
    }

    override suspend fun getUser(userId: UUID): GetUserResponse {
        val response = httpClient.get("$evearaApiBaseUrl/users/$userId") {
            accept(ContentType.Application.Json)
            bearerAuth(getEvearaApiToken())
            parameter("search_term", "")
            parameter("uuid", userId)
        }
        if (!response.status.isSuccess()) {
            throw ServerResponseException(response, "Error getting user!")
        }
        val getUserResponse: GetUserResponse = response.body()
        if (!getUserResponse.success) {
            throw ServerResponseException(response, "Error getting user! success==false")
        }

        return getUserResponse
    }

    override suspend fun addUserSubscription(user: User): AddUserSubscriptionResponse {
        require(user.distributionSubscriptionId == null) { "User.distributionSubscriptionId must be null!" }
        requireNotNull(user.id) { "User.id must not be null!" }
        requireNotNull(user.distributionUserId) { "User.distributionUserId must not be null!" }
        val response = httpClient.post("$evearaApiBaseUrl/subscriptions") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(getEvearaApiToken())
            setBody(
                AddUserSubscriptionRequest(
                    uuid = user.distributionUserId!!,
                    subscriptions = listOf(
                        Subscription(
                            subscriptionId = configRepository.getLong(CONFIG_KEY_EVEARA_PARTNER_SUBSCRIPTION_ID),
                            partnerReferenceId = user.id.toString(),
                        )
                    )
                )
            )
        }
        if (!response.status.isSuccess()) {
            throw ServerResponseException(response, "Error adding user subscription!")
        }
        val addUserSubscriptionResponse: AddUserSubscriptionResponse = response.body()
        if (!addUserSubscriptionResponse.success) {
            throw ServerResponseException(response, "Error adding user subscription! success==false")
        }

        return addUserSubscriptionResponse
    }

    override suspend fun getUserSubscription(user: User): GetUserSubscriptionResponse {
        val response = httpClient.get("$evearaApiBaseUrl/subscriptions") {
            accept(ContentType.Application.Json)
            bearerAuth(getEvearaApiToken())
            parameter("uuid", user.distributionUserId)
        }
        if (!response.status.isSuccess()) {
            throw ServerResponseException(response, "Error getting user subscription!")
        }
        val getUserSubscriptionResponse: GetUserSubscriptionResponse = response.body()
        if (!getUserSubscriptionResponse.success) {
            throw ServerResponseException(response, "Error getting user subscription! success==false")
        }

        if (getUserSubscriptionResponse.subscriptions.isEmpty()) {
            throw ServerResponseException(response, "Error getting user subscription! subscriptions.isEmpty()")
        }

        return getUserSubscriptionResponse
    }

    override suspend fun addUserLabel(user: User): AddUserLabelResponse {
        require(user.companyIpRights == true) { "User.companyIpRights must be true!" }
        requireNotNull(user.distributionUserId) { "User.distributionUserId must not be null!" }
        requireNotNull(user.companyName) { "User.companyName must not be null!" }
        val response = httpClient.post("$evearaApiBaseUrl/labels") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(getEvearaApiToken())
            setBody(
                AddUserLabelRequest(
                    uuid = user.distributionUserId!!,
                    name = user.companyName,
                )
            )
        }
        if (!response.status.isSuccess()) {
            throw ServerResponseException(response, "Error adding user label!")
        }
        val addUserLabelResponse: AddUserLabelResponse = response.body()
        if (!addUserLabelResponse.success) {
            throw ServerResponseException(response, "Error adding user label! success==false")
        }

        return addUserLabelResponse
    }

    override suspend fun updateUserLabel(user: User): UpdateUserLabelResponse {
        require(user.companyIpRights == true) { "User.companyIpRights must be true!" }
        requireNotNull(user.distributionLabelId) { "User.distributionLabelId must not be null!" }
        requireNotNull(user.companyName) { "User.companyName must not be null!" }
        val response = httpClient.put("$evearaApiBaseUrl/labels/${user.distributionLabelId}") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(getEvearaApiToken())
            setBody(
                AddUserLabelRequest(
                    uuid = user.distributionUserId!!,
                    name = user.companyName,
                )
            )
        }
        if (!response.status.isSuccess()) {
            throw ServerResponseException(response, "Error updating user label!")
        }
        val updateUserLabelResponse: UpdateUserLabelResponse = response.body()
        if (!updateUserLabelResponse.success) {
            throw ServerResponseException(response, "Error updating user label! success==false")
        }

        return updateUserLabelResponse
    }

    override suspend fun deleteUserLabel(user: User): DeleteUserLabelResponse {
        require(user.companyIpRights == true) { "User.companyIpRights must be true!" }
        requireNotNull(user.distributionLabelId) { "User.distributionLabelId must not be null!" }
        val response = httpClient.delete("$evearaApiBaseUrl/labels/${user.distributionLabelId}") {
            accept(ContentType.Application.Json)
            bearerAuth(getEvearaApiToken())
        }
        if (!response.status.isSuccess()) {
            throw ServerResponseException(response, "Error deleting user label!")
        }
        val deleteUserLabelResponse: DeleteUserLabelResponse = response.body()
        if (!deleteUserLabelResponse.success) {
            throw ServerResponseException(response, "Error deleting user label! success==false")
        }

        return deleteUserLabelResponse
    }

    override suspend fun addArtist(addArtistRequest: AddArtistRequest): AddArtistResponse {
        val response = httpClient.post("$evearaApiBaseUrl/artists") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(getEvearaApiToken())
            setBody(addArtistRequest)
        }
        if (!response.status.isSuccess()) {
            throw ServerResponseException(response, "Error adding artist!")
        }
        val addArtistResponse: AddArtistResponse = response.body()
        if (!addArtistResponse.success) {
            throw ServerResponseException(response, "Error adding artist! success==false")
        }

        return addArtistResponse
    }

    override suspend fun getArtist(artistId: String): GetArtistResponse {
        val response = httpClient.get("$evearaApiBaseUrl/artists/$artistId") {
            accept(ContentType.Application.Json)
            bearerAuth(getEvearaApiToken())
        }
        if (!response.status.isSuccess()) {
            throw ServerResponseException(response, "Error getting artist!")
        }
        val getArtistResponse: GetArtistResponse = response.body()
        if (!getArtistResponse.success) {
            throw ServerResponseException(response, "Error getting artist! success==false")
        }

        return getArtistResponse
    }

    override suspend fun addParticipant(user: User): AddParticipantResponse {
        if (user.distributionParticipantId != null) {
            return AddParticipantResponse(
                success = true,
                message = "User already has a participantId!",
                participantId = user.distributionParticipantId!!
            )
        }

        requireNotNull(user.distributionUserId) { "User.distributionUserId must not be null!" }
        val response = httpClient.post("$evearaApiBaseUrl/participants") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(getEvearaApiToken())
            setBody(
                AddParticipantRequest(
                    uuid = user.distributionUserId!!,
                    name = user.stageOrFullName,
                    isni = user.distributionIsni,
                    ipn = user.distributionIpn,
                )
            )
        }
        if (!response.status.isSuccess()) {
            throw ServerResponseException(response, "Error adding participant!")
        }
        val addParticipantResponse: AddParticipantResponse = response.body()
        if (!addParticipantResponse.success) {
            throw ServerResponseException(response, "Error adding participant! success==false")
        }

        return addParticipantResponse
    }

    override suspend fun updateParticipant(user: User): AddParticipantResponse {
        requireNotNull(user.distributionParticipantId) { "User.distributionParticipantId must not be null!" }
        val response = httpClient.put("$evearaApiBaseUrl/participants/${user.distributionParticipantId}") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(getEvearaApiToken())
            setBody(
                AddParticipantRequest(
                    uuid = user.distributionUserId!!,
                    name = user.stageOrFullName,
                    isni = user.distributionIsni,
                    ipn = user.distributionIpn,
                )
            )
        }
        if (!response.status.isSuccess()) {
            throw ServerResponseException(response, "Error updating participant!")
        }
        val addParticipantResponse: AddParticipantResponse = response.body()
        if (!addParticipantResponse.success) {
            throw ServerResponseException(response, "Error updating participant! success==false")
        }

        return addParticipantResponse
    }

    override suspend fun addParticipantPaypal(user: User, paypalEmail: String): AddParticipantPaypalResponse {
        requireNotNull(user.distributionUserId) { "User.distributionUserId must not be null!" }
        requireNotNull(user.distributionParticipantId) { "User.distributionParticipantId must not be null!" }
        val response = httpClient.post("$evearaApiBaseUrl/participants/paypal/${user.distributionParticipantId}") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(getEvearaApiToken())
            setBody(
                AddParticipantPaypalRequest(
                    uuid = user.distributionUserId!!,
                    paypalEmailId = paypalEmail,
                )
            )
        }
        if (!response.status.isSuccess()) {
            throw ServerResponseException(response, "Error adding participant paypal!")
        }
        val addParticipantPaypalResponse: AddParticipantPaypalResponse = response.body()
        if (!addParticipantPaypalResponse.success) {
            throw ServerResponseException(response, "Error adding participant paypal! success==false")
        }

        return addParticipantPaypalResponse
    }

    override suspend fun addTrack(evearaUserId: UUID, trackFile: File): AddTrackResponse {
        val response = httpClient.post("$evearaApiBaseUrl/tracks") {
            contentType(ContentType.MultiPart.FormData)
            accept(ContentType.Application.Json)
            bearerAuth(getEvearaApiToken())
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("uuid", evearaUserId.toString())
                        append(
                            "track_file",
                            InputProvider { trackFile.inputStream().asInput() },
                            Headers.build {
                                append(HttpHeaders.ContentDisposition, "filename=${trackFile.name}")
                            }
                        )
                    }
                )
            )
        }
        if (!response.status.isSuccess()) {
            throw ServerResponseException(response, "Error adding track!")
        }
        val addTrackResponse: AddTrackResponse = response.body()
        if (!addTrackResponse.success) {
            throw ServerResponseException(response, "Error adding track! success==false")
        }

        return addTrackResponse
    }

    override suspend fun updateTrack(trackId: Long, user: User, song: Song): UpdateTrackResponse {
        val genres = getGenres().genres
        val languages = getLanguages().languages
        val collabs = collabRepository.getAll(
            user.id!!,
            CollaborationFilters(
                inbound = null,
                songIds = listOf(song.id!!),
                olderThan = null,
                newerThan = null,
                ids = null,
                statuses = null,
            ),
            0,
            Integer.MAX_VALUE
        )
        require(collabs.all { it.status == CollaborationStatus.Accepted }) { "All Collaborations must be accepted!" }

        val response = httpClient.put("$evearaApiBaseUrl/tracks/$trackId") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(getEvearaApiToken())
            setBody(
                UpdateTrackRequest(
                    uuid = user.id,
                    trackFileName = song.originalAudioUrl!!.substringAfterLast("/"),
                    stereoIsrc = song.isrc,
                    iswc = song.iswc,
                    genre = song.genres?.mapNotNull { songGenreName -> genres.firstOrNull { it.name == songGenreName }?.genreId },
                    language = languages.find { it.name == song.language }?.languageCode,
                    explicit = if (song.parentalAdvisory.equals("Non-Explicit", ignoreCase = true)) {
                        0 // Clean
                    } else {
                        1 // Explicit
                    },
                    artists = (
                        listOf(user.distributionArtistId!!) +
                            collabs.filter { it.role == "Artist" && it.featured == false && it.email != user.email }
                                .map { UserEntity.getByEmail(it.email!!)!!.distributionArtistId!! }
                        ).distinct(),
                    featuredArtists = collabs.filter { it.featured == true }
                        .map { UserEntity.getByEmail(it.email!!)!!.distributionArtistId!! }.distinct(),
                    albumOnly = false,
                    lyrics = null, // TODO: Implement lyrics later.
                )
            )
        }
        if (!response.status.isSuccess()) {
            throw ServerResponseException(response, "Error updating track!")
        }
        val updateTrackResponse: UpdateTrackResponse = response.body()
        if (!updateTrackResponse.success) {
            throw ServerResponseException(response, "Error updating track! success==false")
        }

        return updateTrackResponse
    }

    override suspend fun distributeSong(song: SongEntity) {
        // TODO: Implement later. Focus on completing minting code first. Assume we have distributed to Eveara successfully.
    }

    @Serializable
    private data class EvearaApiGetTokenRequest(
        @SerialName("grant_type")
        val grantType: String,
        @SerialName("client_id")
        val clientId: String,
        @SerialName("client_secret")
        val clientSecret: String,
    )

    @Serializable
    private data class EvearaApiTokenResponse(
        @SerialName("success")
        val success: Boolean,
        @SerialName("access_token")
        val accessToken: String,
        @SerialName("refresh_token")
        val refreshToken: String,
        @SerialName("expires_in")
        val expiresInSeconds: Long,
    )

    private data class ApiToken(
        val accessToken: String,
        val refreshToken: String,
        val expiryTimestampMillis: Long,
    )

    companion object {
        private const val GRANT_TYPE = "client_credentials"
        private const val FIVE_MINUTES_IN_MILLIS = 300_000L
    }
}
