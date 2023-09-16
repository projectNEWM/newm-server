package io.newm.server.features.distribution.eveara

import com.amazonaws.HttpMethod
import com.amazonaws.services.s3.AmazonS3
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.timeout
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.forms.InputProvider
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.quote
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.utils.io.core.isNotEmpty
import io.ktor.utils.io.core.readBytes
import io.ktor.utils.io.streams.asInput
import io.newm.server.config.repo.ConfigRepository
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_EVEARA_CLIENT_ID
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_EVEARA_CLIENT_SECRET
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_EVEARA_PARTNER_SUBSCRIPTION_ID
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_EVEARA_SERVER
import io.newm.server.features.collaboration.model.CollaborationStatus
import io.newm.server.features.collaboration.repo.CollaborationRepository
import io.newm.server.features.distribution.DistributionRepository
import io.newm.server.features.distribution.model.AddAlbumRequest
import io.newm.server.features.distribution.model.AddAlbumResponse
import io.newm.server.features.distribution.model.AddArtistRequest
import io.newm.server.features.distribution.model.AddArtistResponse
import io.newm.server.features.distribution.model.AddParticipantRequest
import io.newm.server.features.distribution.model.AddParticipantResponse
import io.newm.server.features.distribution.model.AddTrackResponse
import io.newm.server.features.distribution.model.AddUserLabelRequest
import io.newm.server.features.distribution.model.AddUserLabelResponse
import io.newm.server.features.distribution.model.AddUserRequest
import io.newm.server.features.distribution.model.AddUserResponse
import io.newm.server.features.distribution.model.AddUserSubscriptionRequest
import io.newm.server.features.distribution.model.AddUserSubscriptionResponse
import io.newm.server.features.distribution.model.CoverImage
import io.newm.server.features.distribution.model.DeleteUserLabelResponse
import io.newm.server.features.distribution.model.DistributeFutureReleaseRequest
import io.newm.server.features.distribution.model.DistributeReleaseRequest
import io.newm.server.features.distribution.model.DistributeReleaseResponse
import io.newm.server.features.distribution.model.DistributionOutletReleaseStatusResponse
import io.newm.server.features.distribution.model.EvearaSimpleResponse
import io.newm.server.features.distribution.model.GetAlbumResponse
import io.newm.server.features.distribution.model.GetArtistResponse
import io.newm.server.features.distribution.model.GetCountriesResponse
import io.newm.server.features.distribution.model.GetGenresResponse
import io.newm.server.features.distribution.model.GetLanguagesResponse
import io.newm.server.features.distribution.model.GetOutletsResponse
import io.newm.server.features.distribution.model.GetParticipantsResponse
import io.newm.server.features.distribution.model.GetRolesResponse
import io.newm.server.features.distribution.model.GetTrackStatusResponse
import io.newm.server.features.distribution.model.GetTracksResponse
import io.newm.server.features.distribution.model.GetUserLabelResponse
import io.newm.server.features.distribution.model.GetUserResponse
import io.newm.server.features.distribution.model.GetUserSubscriptionResponse
import io.newm.server.features.distribution.model.OutletsDetail
import io.newm.server.features.distribution.model.Participant
import io.newm.server.features.distribution.model.Preview
import io.newm.server.features.distribution.model.Subscription
import io.newm.server.features.distribution.model.Track
import io.newm.server.features.distribution.model.UpdateArtistRequest
import io.newm.server.features.distribution.model.UpdateArtistResponse
import io.newm.server.features.distribution.model.UpdateTrackRequest
import io.newm.server.features.distribution.model.UpdateUserLabelRequest
import io.newm.server.features.distribution.model.UpdateUserLabelResponse
import io.newm.server.features.distribution.model.UpdateUserRequest
import io.newm.server.features.distribution.model.ValidateAlbumResponse
import io.newm.server.features.song.model.Song
import io.newm.server.features.song.model.SongBarcodeType
import io.newm.server.features.song.model.toSongBarcodeType
import io.newm.server.features.song.repo.SongRepository
import io.newm.server.features.user.model.User
import io.newm.server.features.user.repo.UserRepository
import io.newm.server.ktx.asValidUrl
import io.newm.server.ktx.getFileNameWithExtensionFromUrl
import io.newm.server.ktx.getSecureConfigString
import io.newm.server.ktx.toAudioContentType
import io.newm.server.ktx.toBucketAndKey
import io.newm.server.logging.logRequestJson
import io.newm.shared.exception.HttpServiceUnavailableException
import io.newm.shared.exception.HttpStatusException.Companion.toException
import io.newm.shared.koin.inject
import io.newm.shared.ktx.getConfigString
import io.newm.shared.ktx.info
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.core.parameter.parametersOf
import org.slf4j.Logger
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.UUID
import kotlin.random.Random.Default.nextLong
import kotlin.time.Duration.Companion.minutes

class EvearaDistributionRepositoryImpl(
    private val collabRepository: CollaborationRepository,
    private val configRepository: ConfigRepository,
) : DistributionRepository {
    private val log: Logger by inject { parametersOf(javaClass.simpleName) }
    private val userRepository: UserRepository by inject()
    private val songRepository: SongRepository by inject()
    private val httpClient: HttpClient by inject()
    private val amazonS3: AmazonS3 by inject()
    private val applicationEnvironment: ApplicationEnvironment by inject()
    private val evearaServer by lazy { applicationEnvironment.getConfigString(CONFIG_KEY_EVEARA_SERVER) }
    private val evearaApiBaseUrl by lazy { "https://$evearaServer/api/v2.1" }
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
                throw ServerResponseException(response, "Error getting genres!: ${response.bodyAsText()}")
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
                throw ServerResponseException(response, "Error getting roles!: ${response.bodyAsText()}")
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
                throw ServerResponseException(response, "Error getting languages!: ${response.bodyAsText()}")
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
                throw ServerResponseException(response, "Error getting countries!: ${response.bodyAsText()}")
            }
            val getCountriesResponse: GetCountriesResponse = response.body()
            if (!getCountriesResponse.success) {
                throw ServerResponseException(response, "Error getting countries! success==false")
            }

            getCountriesResponse.also { countriesCache.put(-1, it) }
        }
    }

    override suspend fun getOutlets(user: User): GetOutletsResponse = outletsMutex.withLock {
        outletsCache.getIfPresent(-1) ?: run {
            val response = httpClient.get("$evearaApiBaseUrl/outlets") {
                accept(ContentType.Application.Json)
                bearerAuth(getEvearaApiToken())
                parameter("uuid", user.distributionUserId!!)
            }
            if (!response.status.isSuccess()) {
                throw ServerResponseException(response, "Error getting outlets!: ${response.bodyAsText()}")
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
                    firstName = user.firstName.ifBlank { "---" },
                    lastName = user.lastName.ifBlank { "---" },
                    email = user.email,
                ).logRequestJson(log)
            )
        }
        if (!response.status.isSuccess()) {
            throw ServerResponseException(response, "Error adding user!: ${response.bodyAsText()}")
        }
        val addUserResponse: AddUserResponse = response.body()
        if (!addUserResponse.success) {
            throw ServerResponseException(response, "Error adding user! success==false")
        }

        return addUserResponse
    }

    override suspend fun getUser(user: User): GetUserResponse {
        val response = httpClient.get("$evearaApiBaseUrl/users") {
            accept(ContentType.Application.Json)
            bearerAuth(getEvearaApiToken())
            if (user.distributionUserId != null) {
                parameter("uuid", user.distributionUserId)
            } else {
                parameter("search_term", user.email!!)
            }
        }
        if (!response.status.isSuccess()) {
            throw ServerResponseException(response, "Error getting user!: ${response.bodyAsText()}")
        }
        val getUserResponse: GetUserResponse = response.body()
        if (!getUserResponse.success) {
            throw ServerResponseException(response, "Error getting user! success==false")
        }

        return getUserResponse
    }

    override suspend fun updateUser(user: User): EvearaSimpleResponse {
        requireNotNull(user.distributionUserId) { "User.distributionUserId must not be null!" }
        requireNotNull(user.firstName) { "User.firstName must not be null!" }
        requireNotNull(user.lastName) { "User.lastName must not be null!" }
        requireNotNull(user.email) { "User.email must not be null!" }
        val response = httpClient.put("$evearaApiBaseUrl/users") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(getEvearaApiToken())
            setBody(
                UpdateUserRequest(
                    uuid = user.distributionUserId!!,
                    firstName = user.firstName.ifBlank { "---" },
                    lastName = user.lastName.ifBlank { "---" },
                    email = user.email,
                ).logRequestJson(log)
            )
        }
        if (!response.status.isSuccess()) {
            throw ServerResponseException(response, "Error updating user!: ${response.bodyAsText()}")
        }
        val updateUserResponse: EvearaSimpleResponse = response.body()
        if (!updateUserResponse.success) {
            throw ServerResponseException(response, "Error updating user! success==false")
        }

        return updateUserResponse
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
                            // Add random number so no dups on eveara side
                            partnerReferenceId = "${user.id} - ${nextLong(Long.MAX_VALUE)}",
                        )
                    )
                ).logRequestJson(log)
            )
        }
        if (!response.status.isSuccess()) {
            throw ServerResponseException(response, "Error adding user subscription!: ${response.bodyAsText()}")
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
            throw ServerResponseException(response, "Error getting user subscription!: ${response.bodyAsText()}")
        }
        val getUserSubscriptionResponse: GetUserSubscriptionResponse = response.body()
        if (!getUserSubscriptionResponse.success) {
            throw ServerResponseException(response, "Error getting user subscription! success==false")
        }

        return getUserSubscriptionResponse
    }

    override suspend fun addUserLabel(distributionUserId: String, label: String): AddUserLabelResponse {
        val response = httpClient.post("$evearaApiBaseUrl/labels") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(getEvearaApiToken())
            setBody(
                AddUserLabelRequest(
                    uuid = distributionUserId,
                    name = label,
                ).logRequestJson(log)
            )
        }
        if (!response.status.isSuccess()) {
            throw ServerResponseException(response, "Error adding user label!: ${response.bodyAsText()}")
        }
        val addUserLabelResponse: AddUserLabelResponse = response.body()
        if (!addUserLabelResponse.success) {
            throw ServerResponseException(response, "Error adding user label! success==false")
        }

        return addUserLabelResponse
    }

    override suspend fun updateUserLabel(
        distributionLabelId: Long,
        distributionUserId: String,
        label: String
    ): UpdateUserLabelResponse {
        val response = httpClient.put("$evearaApiBaseUrl/labels/$distributionLabelId") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(getEvearaApiToken())
            setBody(
                UpdateUserLabelRequest(
                    uuid = distributionUserId,
                    name = label,
                ).logRequestJson(log)
            )
        }
        if (!response.status.isSuccess()) {
            throw ServerResponseException(response, "Error updating user label!: ${response.bodyAsText()}")
        }
        val updateUserLabelResponse: UpdateUserLabelResponse = response.body()
        if (!updateUserLabelResponse.success) {
            throw ServerResponseException(response, "Error updating user label! success==false")
        }

        return updateUserLabelResponse
    }

    override suspend fun getUserLabel(user: User): GetUserLabelResponse {
        requireNotNull(user.distributionUserId) { "User.distributionUserId must not be null!" }
        val response = httpClient.get("$evearaApiBaseUrl/labels") {
            accept(ContentType.Application.Json)
            bearerAuth(getEvearaApiToken())
            parameter("uuid", user.distributionUserId)
        }
        if (!response.status.isSuccess()) {
            throw ServerResponseException(response, "Error getting user label!: ${response.bodyAsText()}")
        }
        val getUserLabelResponse: GetUserLabelResponse = response.body()
        if (!getUserLabelResponse.success) {
            throw ServerResponseException(response, "Error getting user label! success==false")
        }

        return getUserLabelResponse
    }

    override suspend fun deleteUserLabel(user: User): DeleteUserLabelResponse {
        require(user.companyIpRights == true) { "User.companyIpRights must be true!" }
        requireNotNull(user.distributionLabelId) { "User.distributionLabelId must not be null!" }
        val response = httpClient.delete("$evearaApiBaseUrl/labels/${user.distributionLabelId}") {
            accept(ContentType.Application.Json)
            bearerAuth(getEvearaApiToken())
        }
        if (!response.status.isSuccess()) {
            throw ServerResponseException(response, "Error deleting user label!: ${response.bodyAsText()}")
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
            throw ServerResponseException(response, "Error adding artist!: ${response.bodyAsText()}")
        }
        val addArtistResponse: AddArtistResponse = response.body()
        if (!addArtistResponse.success) {
            throw ServerResponseException(response, "Error adding artist! success==false")
        }

        return addArtistResponse
    }

    override suspend fun updateArtist(artistId: Long, updateArtistRequest: UpdateArtistRequest): UpdateArtistResponse {
        val response = httpClient.put("$evearaApiBaseUrl/artists/$artistId") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(getEvearaApiToken())
            setBody(updateArtistRequest)
        }
        if (!response.status.isSuccess()) {
            throw ServerResponseException(response, "Error updating artist!")
        }
        val updateArtistResponse: UpdateArtistResponse = response.body()
        if (!updateArtistResponse.success) {
            throw ServerResponseException(response, "Error updating artist! success==false")
        }

        return updateArtistResponse
    }

    override suspend fun getArtist(user: User): GetArtistResponse {
        val response = httpClient.get("$evearaApiBaseUrl/artists/${user.distributionArtistId}") {
            accept(ContentType.Application.Json)
            bearerAuth(getEvearaApiToken())
            parameter("uuid", user.distributionUserId)
        }
        if (!response.status.isSuccess()) {
            throw ServerResponseException(response, "Error getting artist!: ${response.bodyAsText()}")
        }
        val getArtistResponse: GetArtistResponse = response.body()
        if (!getArtistResponse.success) {
            throw ServerResponseException(response, "Error getting artist! success==false")
        }

        return getArtistResponse
    }

    override suspend fun getArtists(user: User): GetArtistResponse {
        val response = httpClient.get("$evearaApiBaseUrl/artists") {
            accept(ContentType.Application.Json)
            bearerAuth(getEvearaApiToken())
            parameter("uuid", user.distributionUserId)
            parameter("limit", 10_000)
        }
        if (!response.status.isSuccess()) {
            throw ServerResponseException(response, "Error getting artists!: ${response.bodyAsText()}")
        }
        val getArtistResponse: GetArtistResponse = response.body()
        if (!getArtistResponse.success) {
            throw ServerResponseException(response, "Error getting artists! success==false")
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
                ).logRequestJson(log)
            )
        }
        if (!response.status.isSuccess()) {
            throw ServerResponseException(response, "Error adding participant!: ${response.bodyAsText()}")
        }
        val addParticipantResponse: AddParticipantResponse = response.body()
        if (!addParticipantResponse.success) {
            throw ServerResponseException(response, "Error adding participant! success==false")
        }

        return addParticipantResponse
    }

    override suspend fun updateParticipant(user: User): EvearaSimpleResponse {
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
            throw ServerResponseException(response, "Error updating participant!: ${response.bodyAsText()}")
        }
        val addParticipantResponse: EvearaSimpleResponse = response.body()
        if (!addParticipantResponse.success) {
            throw ServerResponseException(response, "Error updating participant! success==false")
        }

        return addParticipantResponse
    }

    override suspend fun getParticipants(user: User): GetParticipantsResponse {
        requireNotNull(user.distributionUserId) { "User.distributionUserId must not be null!" }
        val response = httpClient.get("$evearaApiBaseUrl/participants") {
            accept(ContentType.Application.Json)
            bearerAuth(getEvearaApiToken())
            parameter("uuid", user.distributionUserId)
        }
        if (!response.status.isSuccess()) {
            throw ServerResponseException(response, "Error getting participant!: ${response.bodyAsText()}")
        }
        val getParticipantsResponse: GetParticipantsResponse = response.body()
        if (!getParticipantsResponse.success) {
            throw ServerResponseException(response, "Error getting participant! success==false")
        }

        return getParticipantsResponse
    }

    override suspend fun addTrack(user: User, trackFile: File): AddTrackResponse {
        requireNotNull(user.distributionUserId) { "User.distributionUserId must not be null!" }

        val response = httpClient.submitFormWithBinaryData(
            "$evearaApiBaseUrl/tracks",
            formData {
                // Eveara API expects values to be quoted
                append("uuid".quote(), user.distributionUserId.toString())
                append(
                    "track_file".quote(),
                    InputProvider(trackFile.length()) { trackFile.inputStream().asInput() },
                    Headers.build {
                        append(HttpHeaders.ContentType, trackFile.name.toAudioContentType())
                        append(HttpHeaders.ContentDisposition, "filename=${trackFile.name.quote()}")
                    }
                )
            }
        ) {
            bearerAuth(getEvearaApiToken())
            timeout {
                // Increase timeout for large files
                requestTimeoutMillis = 10.minutes.inWholeMilliseconds
            }
            // dump the form data to a file for debugging
            // (body as MultiPartFormDataContent).writeTo(File("/tmp/track-upload-form-data.txt").writeChannel())
        }
        if (!response.status.isSuccess()) {
            throw ServerResponseException(
                response,
                "Error adding track for user uuid: ${user.distributionUserId}!: ${response.bodyAsText()}"
            )
        }
        val addTrackResponse: AddTrackResponse = response.body()
        if (!addTrackResponse.success) {
            throw ServerResponseException(response, "Error adding track! success==false")
        }

        return addTrackResponse
    }

    override suspend fun updateTrack(user: User, trackId: Long, song: Song): EvearaSimpleResponse {
        requireNotNull(user.distributionUserId) { "User.distributionUserId must not be null!" }
        val genres = getGenres().genres
        val languages = getLanguages().languages
        val collabs = collabRepository.getAllBySongId(song.id!!)

        val artistIds = listOf(user.distributionArtistId!!) +
            collabs.filter { it.role == "Artist" && it.featured == false && it.email != user.email }
                .map { it.distributionArtistId!! }.distinct()
        val featuredArtistIds = collabs.filter { it.featured == true && it.email != user.email }
            .map { it.distributionArtistId!! }.distinct().takeIf { it.isNotEmpty() }

        val response = httpClient.put("$evearaApiBaseUrl/tracks/$trackId") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(getEvearaApiToken())
            setBody(
                UpdateTrackRequest(
                    uuid = user.distributionUserId!!,
                    name = song.title!!,
                    stereoIsrc = song.isrc?.replace("-", ""), // Eveara API expects ISRC without dashes
                    iswc = song.iswc,
                    genre = song.genres?.mapNotNull { songGenreName -> genres.firstOrNull { it.name == songGenreName }?.genreId },
                    language = languages.find { it.name == song.language }?.code,
                    explicit = if (song.parentalAdvisory.equals("Non-Explicit", ignoreCase = true)) {
                        null // Clean
                    } else {
                        1 // Explicit
                    },
                    availability = listOf(1, 2), // 1 = Download, 2 = Streaming
                    artists = artistIds,
                    featuredArtists = featuredArtistIds,
                    albumOnly = false,
                    lyrics = null, // TODO: Implement lyrics later.
                ).logRequestJson(log)
            )
        }
        if (!response.status.isSuccess()) {
            throw ServerResponseException(response, "Error updating track!: ${response.bodyAsText()}")
        }
        val updateTrackResponse: EvearaSimpleResponse = response.body()
        if (!updateTrackResponse.success) {
            throw ServerResponseException(response, "Error updating track! success==false")
        }

        return updateTrackResponse
    }

    override suspend fun getTracks(user: User, trackId: Long?): GetTracksResponse {
        requireNotNull(user.distributionUserId) { "User.distributionUserId must not be null!" }
        val response = httpClient.get("$evearaApiBaseUrl/tracks${trackId?.let { "/$trackId" } ?: ""}") {
            accept(ContentType.Application.Json)
            bearerAuth(getEvearaApiToken())
            parameter("uuid", user.distributionUserId)
        }
        if (!response.status.isSuccess()) {
            throw ServerResponseException(response, "Error getting tracks!: ${response.bodyAsText()}")
        }
        val getTracksResponse: GetTracksResponse = response.body()
        if (!getTracksResponse.success) {
            throw ServerResponseException(response, "Error getting tracks! success==false")
        }

        return getTracksResponse
    }

    override suspend fun deleteTrack(user: User, trackId: Long): EvearaSimpleResponse {
        requireNotNull(user.distributionUserId) { "User.distributionUserId must not be null!" }
        val response = httpClient.delete("$evearaApiBaseUrl/tracks/$trackId") {
            accept(ContentType.Application.Json)
            bearerAuth(getEvearaApiToken())
            parameter("uuid", user.distributionUserId)
        }
        if (!response.status.isSuccess()) {
            throw ServerResponseException(response, "Error deleting track!: ${response.bodyAsText()}")
        }
        val deleteTrackResponse: EvearaSimpleResponse = response.body()
        if (!deleteTrackResponse.success) {
            throw ServerResponseException(response, "Error deleting track! success==false")
        }

        return deleteTrackResponse
    }

    override suspend fun isTrackStatusCompleted(user: User, trackId: Long): Boolean {
        val response = httpClient.get("$evearaApiBaseUrl/tracks/$trackId/status") {
            accept(ContentType.Application.Json)
            bearerAuth(getEvearaApiToken())
            parameter("uuid", user.distributionUserId!!)
        }
        if (!response.status.isSuccess()) {
            throw ServerResponseException(response, "Error getting track status!")
        }
        val getTrackStatusResponse: GetTrackStatusResponse = response.body()
        if (!getTrackStatusResponse.success) {
            throw ServerResponseException(response, "Error getting track status!: ${response.bodyAsText()}")
        }

        log.info { "Track status: ${getTrackStatusResponse.trackStatus.stereo}" }

        return getTrackStatusResponse.trackStatus.stereo?.equals("Completed", ignoreCase = true) ?: false
    }

    override suspend fun addAlbum(user: User, trackId: Long, song: Song): AddAlbumResponse {
        requireNotNull(user.distributionUserId) { "User.distributionUserId must not be null!" }
        val collabs = collabRepository.getAllBySongId(song.id!!)
        val artistIds = listOf(user.distributionArtistId!!) +
            collabs.filter { it.role == "Artist" && it.featured == false && it.email != user.email }
                .map { it.distributionArtistId!! }.distinct()
        val featuredArtistIds = collabs.filter { it.featured == true && it.email != user.email }
            .map { it.distributionArtistId!! }.distinct().takeIf { it.isNotEmpty() }
        val response = httpClient.post("$evearaApiBaseUrl/albums") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(getEvearaApiToken())
            setBody(
                AddAlbumRequest(
                    uuid = user.distributionUserId!!,
                    name = song.title, // NOTE: for single, track title is used as album name
                    artistIds = artistIds,
                    subscriptionId = user.distributionSubscriptionId!!,
                    eanUpc = song.barcodeNumber,
                    productCodeType = if (song.barcodeNumber == null || song.barcodeType == SongBarcodeType.Ean) {
                        "ean"
                    } else if (song.barcodeType == SongBarcodeType.Upc) {
                        "upc"
                    } else {
                        "jan"
                    },
                    labelId = user.distributionLabelId,
                    productType = "single",
                    codeAutoGenerate = song.barcodeNumber == null,
                    productFormat = "stereo",
                    coverImage = CoverImage(
                        url = song.coverArtUrl.asValidUrl(),
                        extension = song.coverArtUrl.asValidUrl().substringAfterLast(".").lowercase(),
                    ),
                    originalReleaseDate = song.releaseDate,
                    tracks = listOf(
                        Track(
                            trackId = trackId,
                            artistIds = artistIds,
                            featuredArtists = featuredArtistIds,
                            preview = Preview(startAt = 0, duration = 30), // TODO: Allow user to specify this later
                            participants = listOf(
                                Participant(
                                    id = user.distributionNewmParticipantId!!,
                                    roleId = listOf(
                                        getRoles().roles.first {
                                            it.name.equals(
                                                "Publisher",
                                                ignoreCase = true
                                            )
                                        }.roleId
                                    ),
                                    payoutSharePercentage = 100, // NEWM takes 100% so we can then pay out based on stream token holdings
                                ),
                                Participant(
                                    id = user.distributionParticipantId!!,
                                    roleId = listOf(
                                        getRoles().roles.first {
                                            it.name.equals(
                                                "Artist",
                                                ignoreCase = true
                                            )
                                        }.roleId
                                    ),
                                    payoutSharePercentage = 0,
                                )
                            )
                        )
                    ),
                ).logRequestJson(log)
            )
        }
        if (!response.status.isSuccess()) {
            throw ServerResponseException(response, "Error adding album!: ${response.bodyAsText()}")
        }
        val addAlbumResponse: AddAlbumResponse = response.body()
        if (!addAlbumResponse.success) {
            throw ServerResponseException(response, "Error adding album! success==false")
        }

        return addAlbumResponse
    }

    override suspend fun getAlbums(user: User): GetAlbumResponse {
        requireNotNull(user.distributionUserId) { "User.distributionUserId must not be null!" }
        val response = httpClient.get("$evearaApiBaseUrl/albums") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(getEvearaApiToken())
            parameter("uuid", user.distributionUserId!!)
            parameter("limit", 10_000)
        }
        if (!response.status.isSuccess()) {
            throw ServerResponseException(response, "Error getting album!: ${response.bodyAsText()}")
        }
        val getAlbumResponse: GetAlbumResponse = response.body()
        if (!getAlbumResponse.success) {
            throw ServerResponseException(response, "Error getting album! success==false")
        }

        return getAlbumResponse
    }

    override suspend fun updateAlbum(user: User, trackId: Long, song: Song): EvearaSimpleResponse {
        requireNotNull(user.distributionUserId) { "User.distributionUserId must not be null!" }
        requireNotNull(song.distributionReleaseId) { "Song.distributionReleaseId must not be null!" }
        val collabs = collabRepository.getAllBySongId(song.id!!)

        val artistIds = listOf(user.distributionArtistId!!) +
            collabs.filter { it.role == "Artist" && it.featured == false && it.email != user.email }
                .map { it.distributionArtistId!! }.distinct()
        val featuredArtistIds = collabs.filter { it.featured == true && it.email != user.email }
            .map { it.distributionArtistId!! }.distinct().takeIf { it.isNotEmpty() }
        val response = httpClient.put("$evearaApiBaseUrl/albums/${song.distributionReleaseId}") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(getEvearaApiToken())
            setBody(
                AddAlbumRequest(
                    uuid = user.distributionUserId!!,
                    name = song.album ?: song.title, // NOTE: for single, track title is used as album name
                    artistIds = artistIds,
                    subscriptionId = user.distributionSubscriptionId!!,
                    eanUpc = song.barcodeNumber,
                    productCodeType = if (song.barcodeNumber == null || song.barcodeType == SongBarcodeType.Ean) {
                        "ean"
                    } else if (song.barcodeType == SongBarcodeType.Upc) {
                        "upc"
                    } else {
                        "jan"
                    },
                    labelId = user.distributionLabelId,
                    productType = "single",
                    codeAutoGenerate = song.barcodeNumber == null,
                    productFormat = "stereo",
                    coverImage = CoverImage(
                        url = song.coverArtUrl.asValidUrl(),
                        extension = song.coverArtUrl.asValidUrl().substringAfterLast(".").lowercase(),
                    ),
                    originalReleaseDate = song.releaseDate,
                    tracks = listOf(
                        Track(
                            trackId = trackId,
                            artistIds = artistIds,
                            featuredArtists = featuredArtistIds,
                            preview = Preview(startAt = 0, duration = 30), // TODO: Allow user to specify this later
                            participants = listOf(
                                Participant(
                                    id = user.distributionNewmParticipantId!!,
                                    roleId = listOf(
                                        getRoles().roles.first {
                                            it.name.equals(
                                                "Publisher",
                                                ignoreCase = true
                                            )
                                        }.roleId
                                    ),
                                    payoutSharePercentage = 100, // NEWM takes 100% so we can then pay out based on stream token holdings
                                ),
                                Participant(
                                    id = user.distributionParticipantId!!,
                                    roleId = listOf(
                                        getRoles().roles.first {
                                            it.name.equals(
                                                "Artist",
                                                ignoreCase = true
                                            )
                                        }.roleId
                                    ),
                                    payoutSharePercentage = 0,
                                )
                            )
                        )
                    ),
                )
            )
        }
        if (!response.status.isSuccess()) {
            throw ServerResponseException(response, "Error updating album!: ${response.bodyAsText()}")
        }
        val updateAlbumResponse: EvearaSimpleResponse = response.body()
        if (!updateAlbumResponse.success) {
            throw ServerResponseException(response, "Error updating album! success==false")
        }

        return updateAlbumResponse
    }

    override suspend fun validateAlbum(user: User, releaseId: Long): ValidateAlbumResponse {
        requireNotNull(user.distributionUserId) { "User.distributionUserId must not be null!" }
        val response = httpClient.get("$evearaApiBaseUrl/albums/$releaseId/validate") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(getEvearaApiToken())
            parameter("uuid", user.distributionUserId!!)
        }
        if (!response.status.isSuccess()) {
            throw ServerResponseException(response, "Error validating album!: ${response.bodyAsText()}")
        }
        val validateAlbumResponse: ValidateAlbumResponse = response.body()
        if (!validateAlbumResponse.success) {
            throw ServerResponseException(response, "Error validating album! success==false")
        }

        return validateAlbumResponse
    }

    override suspend fun deleteAlbum(user: User, releaseId: Long): EvearaSimpleResponse {
        requireNotNull(user.distributionUserId) { "User.distributionUserId must not be null!" }
        val response = httpClient.delete("$evearaApiBaseUrl/albums/$releaseId") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(getEvearaApiToken())
            parameter("uuid", user.distributionUserId!!)
        }
        if (!response.status.isSuccess()) {
            throw ServerResponseException(response, "Error deleting album!: ${response.bodyAsText()}")
        }
        val deleteAlbumResponse: EvearaSimpleResponse = response.body()
        if (!deleteAlbumResponse.success) {
            throw ServerResponseException(response, "Error deleting album! success==false")
        }

        return deleteAlbumResponse
    }

    override suspend fun simulateDistributeRelease(user: User, releaseId: Long): EvearaSimpleResponse {
        requireNotNull(user.distributionUserId) { "User.distributionUserId must not be null!" }
        val response = httpClient.put("$evearaApiBaseUrl/simulate/distribute") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(getEvearaApiToken())
            setBody(
                DistributeReleaseRequest(
                    uuid = user.distributionUserId!!,
                    releaseId = releaseId,
                ).logRequestJson(log)
            )
        }
        if (!response.status.isSuccess()) {
            throw ServerResponseException(response, "Error simulating distribute release!: ${response.bodyAsText()}")
        }
        val distributeReleaseResponse: EvearaSimpleResponse = response.body()
        if (!distributeReleaseResponse.success) {
            throw ServerResponseException(response, "Error simulating distribute release! success==false")
        }

        return distributeReleaseResponse
    }

    override suspend fun distributeReleaseToOutlets(
        user: User,
        releaseStartDate: LocalDate,
        releaseId: Long
    ): DistributeReleaseResponse {
        requireNotNull(user.distributionUserId) { "User.distributionUserId must not be null!" }
        val response = httpClient.patch("$evearaApiBaseUrl/outlets/$releaseId/distribute") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(getEvearaApiToken())
            setBody(
                DistributeReleaseRequest(
                    uuid = user.distributionUserId!!,
                    outletsDetails = getOutlets(user).outlets.map { evearaOutlet ->
                        OutletsDetail(
                            storeId = evearaOutlet.storeId,
                            releaseStartDate = releaseStartDate,
                        )
                    }
                ).logRequestJson(log)
            )
        }
        if (!response.status.isSuccess()) {
            throw ServerResponseException(response, "Error distributing release to outlets!: ${response.bodyAsText()}")
        }
        val distributeReleaseResponse: DistributeReleaseResponse = response.body()
        if (!distributeReleaseResponse.success) {
            throw ServerResponseException(response, "Error distributing release to outlets! success==false")
        }

        return distributeReleaseResponse
    }

    override suspend fun distributeReleaseToFutureOutlets(user: User, releaseId: Long): DistributeReleaseResponse {
        requireNotNull(user.distributionUserId) { "User.distributionUserId must not be null!" }
        val response = httpClient.patch("$evearaApiBaseUrl/outlets/$releaseId/future-outlets") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(getEvearaApiToken())
            setBody(
                DistributeFutureReleaseRequest(
                    uuid = user.distributionUserId!!,
                    enableDistributeToFutureOutlets = true,
                )
            )
        }
        if (!response.status.isSuccess()) {
            throw ServerResponseException(
                response,
                "Error distributing release to future-outlets!: ${response.bodyAsText()}"
            )
        }
        val distributeReleaseResponse: DistributeReleaseResponse = response.body()
        if (!distributeReleaseResponse.success) {
            throw ServerResponseException(response, "Error distributing release to future-outlets! success==false")
        }

        return distributeReleaseResponse
    }

    override suspend fun distributionOutletReleaseStatus(
        user: User,
        releaseId: Long
    ): DistributionOutletReleaseStatusResponse {
        requireNotNull(user.distributionUserId) { "User.distributionUserId must not be null!" }
        val response = httpClient.get("$evearaApiBaseUrl/outlets/$releaseId") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(getEvearaApiToken())
            parameter("uuid", user.distributionUserId!!)
        }
        if (!response.status.isSuccess()) {
            throw ServerResponseException(
                response,
                "Error getting distribution outlet release status!: ${response.bodyAsText()}"
            )
        }
        val distributionOutletReleaseStatusResponse: DistributionOutletReleaseStatusResponse = response.body()
        if (!distributionOutletReleaseStatusResponse.success) {
            throw ServerResponseException(response, "Error getting distribution outlet release status! success==false")
        }

        return distributionOutletReleaseStatusResponse
    }

    override suspend fun distributeSong(song: Song) {
        var mutableSong = song
        requireNotNull(mutableSong.ownerId) { "Song.ownerId must not be null!" }
        val user = userRepository.get(mutableSong.ownerId!!)
        requireNotNull(user.id) { "User.id must not be null!" }
        createDistributionUserIfNeeded(user)

        val collabs = collabRepository.getAllBySongId(mutableSong.id!!)
        require(collabs.all { it.status == CollaborationStatus.Accepted }) { "All Collaborations must be accepted!" }

        // Create the distribution artistId for each collaborator if they don't exist yet
        val collabDistributionArtists = getArtists(user).artists.associate { it.name to it.artistId }.toMutableMap()
        collabs.forEach { collab ->
            if (collab.distributionArtistId == null) {
                val collabUser = userRepository.findByEmail(collab.email!!)

                collabDistributionArtists[collabUser.stageOrFullName]?.let { distributionArtistId ->
                    log.info { "Found existing collab distribution artist ${collabUser.email} with id $distributionArtistId" }
                    collabRepository.update(
                        collab.copy(distributionArtistId = distributionArtistId),
                        collab.id!!,
                        user.id,
                        skipStatusCheck = true
                    )
                } ?: run {
                    // FIXME: don't hardcode artist's country
                    val hardcodedCountry =
                        getCountries().countries.first { it.countryCode.equals("us", ignoreCase = true) }.countryCode
                    val response =
                        addArtist(
                            AddArtistRequest(
                                user.distributionUserId!!,
                                collabUser.stageOrFullName,
                                hardcodedCountry
                            ).logRequestJson(log)
                        )
                    log.info { "Created collab distribution artist ${collabUser.email} with id ${response.artistId}: ${response.message}" }
                    collabRepository.update(
                        collab.copy(distributionArtistId = response.artistId),
                        collab.id!!,
                        user.id,
                        skipStatusCheck = true
                    )
                    collabDistributionArtists[collabUser.stageOrFullName] = response.artistId
                }
            }
        }

        // Create the distribution subscription if it doesn't yet exist
        val getSubscriptionResponse = getUserSubscription(user)
        if (getSubscriptionResponse.totalRecords > 0) {
            val existingSubscription = getSubscriptionResponse.subscriptions.first()
            log.info { "Found existing distribution subscription ${user.email} with id ${existingSubscription.mySubscriptionId}" }
            if (user.distributionSubscriptionId == null) {
                user.distributionSubscriptionId = existingSubscription.mySubscriptionId
                userRepository.updateUserData(user.id, user)
            } else {
                require(user.distributionSubscriptionId == existingSubscription.mySubscriptionId) { "User.distributionSubscriptionId: ${user.distributionSubscriptionId} does not match existing distribution subscription! ${existingSubscription.mySubscriptionId}" }
            }
        } else {
            require(user.distributionSubscriptionId == null) { "User.distributionSubscriptionId: ${user.distributionSubscriptionId} not found in Eveara!" }
            val response = addUserSubscription(user)
            log.info { "Created distribution subscription ${user.email} with id ${response.subscriptions[0].userSubscriptionId}: ${response.message}" }
            user.distributionSubscriptionId = response.subscriptions[0].userSubscriptionId
            userRepository.updateUserData(user.id, user)
        }

        // Add Distribution Artist if they don't yet exist
        val existingDistributionArtist = collabDistributionArtists[user.stageOrFullName]
        if (user.distributionArtistId == null) {
            if (existingDistributionArtist != null) {
                log.info { "Found existing distribution artist ${user.email} with id $existingDistributionArtist" }
                user.distributionArtistId = existingDistributionArtist
                userRepository.updateUserData(user.id, user)
            } else {
                // FIXME: don't hardcode artist's country
                val hardcodedCountry =
                    getCountries().countries.first { it.countryCode.equals("us", ignoreCase = true) }.countryCode
                val response =
                    addArtist(AddArtistRequest(user.distributionUserId!!, user.stageOrFullName, hardcodedCountry))
                log.info { "Created distribution artist ${user.email} with id ${response.artistId}: ${response.message}" }
                user.distributionArtistId = response.artistId
                userRepository.updateUserData(user.id, user)
            }
        }

        // Create the newm participant if they don't exist yet
        val getParticipantsResponse = getParticipants(user)
        if (getParticipantsResponse.totalRecords > 0) {
            val existingNewmParticipant = getParticipantsResponse.participantData.first { it.name == "NEWM" }
            log.info { "Found existing distribution participant NEWM with id ${existingNewmParticipant.participantId}" }
            if (user.distributionNewmParticipantId == null) {
                user.distributionNewmParticipantId = existingNewmParticipant.participantId
                userRepository.updateUserData(user.id, user)
            } else {
                require(user.distributionNewmParticipantId == existingNewmParticipant.participantId) { "User.distributionNewmParticipantId: ${user.distributionNewmParticipantId} does not match existing distribution participant! ${existingNewmParticipant.participantId}" }
            }
        } else {
            require(user.distributionNewmParticipantId == null) { "User.distributionNewmParticipantId: ${user.distributionNewmParticipantId} not found in Eveara!" }
            val response = addParticipant(User(distributionUserId = user.distributionUserId!!, nickname = "NEWM"))
            log.info { "Created NEWM participant with id ${response.participantId}: ${response.message}" }
            user.distributionNewmParticipantId = response.participantId
            userRepository.updateUserData(user.id, user)
        }

        // Create/Update the participant (will pay to newm any royalties)
        if (getParticipantsResponse.totalRecords > 0) {
            val existingParticipant = getParticipantsResponse.participantData.first { it.name == user.stageOrFullName }
            log.info { "Found existing distribution participant ${user.email} with id ${existingParticipant.participantId}" }
            if (user.distributionParticipantId == null) {
                user.distributionParticipantId = existingParticipant.participantId
                userRepository.updateUserData(user.id, user)
            } else {
                require(user.distributionParticipantId == existingParticipant.participantId) { "User.distributionParticipantId: ${user.distributionParticipantId} does not match existing distribution participant! ${existingParticipant.participantId}" }
            }
            if (existingParticipant.name != user.stageOrFullName || existingParticipant.ipn.ifBlank { null } != user.distributionIpn || existingParticipant.isni.ifBlank { null } != user.distributionIsni) {
                log.info { "Updating distribution participant ${user.email} with id ${existingParticipant.participantId}, name ${existingParticipant.name} -> ${user.stageOrFullName}, ipn ${existingParticipant.ipn} -> ${user.distributionIpn}, isni ${existingParticipant.isni} -> ${user.distributionIsni}" }
                val response = updateParticipant(user)
                log.info { "Updated distribution participant ${user.email} with id ${user.distributionUserId}: ${response.message}" }
            }
        } else {
            require(user.distributionParticipantId == null) { "User.distributionParticipantId: ${user.distributionParticipantId} not found in Eveara!" }
            val response = addParticipant(user)
            log.info { "Created distribution participant ${user.email} with id ${response.participantId}: ${response.message}" }
            user.distributionParticipantId = response.participantId
            userRepository.updateUserData(user.id, user)
        }

        // Add or Update Distribution Label
        val desiredLabelName = song.phonographicCopyrightOwner ?: user.companyOrStageOrFullName
        val getUserLabelsResponse = getUserLabel(user)
        if (getUserLabelsResponse.totalRecords > 1) {
            val existingLabel = getUserLabelsResponse.userLabelData.first { it.name != "NEWM" } // TODO: FIX hardcoding
            log.info { "Found existing distribution label ${user.email} with id ${existingLabel.labelId}, name ${existingLabel.name}" }
            if (user.distributionLabelId == null) {
                user.distributionLabelId = existingLabel.labelId
                userRepository.updateUserData(user.id, user)
            } else {
                require(user.distributionLabelId == existingLabel.labelId) { "User.distributionLabelId: ${user.distributionLabelId} does not match existing distribution label! ${existingLabel.labelId}" }
            }
            if (existingLabel.name != desiredLabelName) {
                log.info { "Updating distribution label ${user.email} with id ${existingLabel.labelId}, name ${existingLabel.name} -> $desiredLabelName" }
                requireNotNull(user.distributionLabelId) { "User.distributionLabelId must not be null!" }
                requireNotNull(user.distributionUserId) { "User.distributionUserId must not be null!" }
                val response = updateUserLabel(
                    user.distributionLabelId!!,
                    user.distributionUserId!!,
                    desiredLabelName,
                )
                log.info { "Updated distribution label ${user.email} with id ${response.labelData?.labelId}: ${response.message}" }
            }
        } else {
            require(user.distributionLabelId == null) { "User.distributionLabelId: ${user.distributionLabelId} not found in Eveara!" }
            requireNotNull(user.distributionUserId) { "User.distributionUserId must not be null!" }
            val response = addUserLabel(
                user.distributionUserId!!,
                desiredLabelName,
            )
            log.info { "Created distribution label ${user.email} with id ${response.labelId}: ${response.message}" }
            user.distributionLabelId = response.labelId
            userRepository.updateUserData(user.id, user)
        }

        // Delete any track that already has this song isrc and is associated with this user
        val getTracksResponse = getTracks(user)
        val existingTrack =
            getTracksResponse.trackData?.firstOrNull { it.stereoIsrc == mutableSong.isrc?.replace("-", "") }
        if (existingTrack != null) {
            val getAlbumResponse = getAlbums(user)
            if (getAlbumResponse.totalRecords > 0) {
                val existingAlbum = getAlbumResponse.albumData.firstOrNull { it.name == mutableSong.title }
                if (existingAlbum != null) {
                    // Delete the album since the track is attached to it. It will be re-created later
                    log.info { "Found existing distribution album ${mutableSong.title} with id ${existingAlbum.releaseId}" }
                    val response = deleteAlbum(user, existingAlbum.releaseId)
                    log.info { "Deleted distribution album ${mutableSong.title} with id ${existingAlbum.releaseId}: ${response.message}" }
                    mutableSong = mutableSong.copy(distributionReleaseId = null)
                    songRepository.update(mutableSong.id!!, mutableSong)
                }
            }
            log.info { "Found existing distribution track ${mutableSong.isrc} with id ${existingTrack.trackId}" }
            val response = deleteTrack(user, existingTrack.trackId)
            log.info { "Deleted distribution track ${mutableSong.isrc} with id ${existingTrack.trackId}: ${response.message}" }
            mutableSong = mutableSong.copy(distributionTrackId = null)
            songRepository.update(mutableSong.id!!, mutableSong)
        }

        // Upload and add metadata to the distribution track
        if (mutableSong.distributionTrackId == null) {
            val s3Url = mutableSong.originalAudioUrl!!
            val (bucket, key) = s3Url.toBucketAndKey()
            val url = amazonS3.generatePresignedUrl(
                bucket,
                key,
                Date.from(Instant.now().plus(30, ChronoUnit.MINUTES)),
                HttpMethod.GET
            ).toExternalForm()
            log.info { "Generated pre-signed url for $s3Url: $url" }

            val audioFileResponse = httpClient.get(url) {
                accept(ContentType.Application.OctetStream)
            }

            if (!audioFileResponse.status.isSuccess()) {
                throw audioFileResponse.status.toException("Error downloading url: $url")
            }

            val trackFile = File.createTempFile("newm_track_", url.getFileNameWithExtensionFromUrl())
            val channel = audioFileResponse.bodyAsChannel()
            while (!channel.isClosedForRead) {
                val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
                while (packet.isNotEmpty) {
                    val bytes = packet.readBytes()
                    trackFile.appendBytes(bytes)
                }
            }
            log.info { "Downloaded track ${mutableSong.title} to ${trackFile.absolutePath} having size ${trackFile.length()}" }
            val response = addTrack(user, trackFile)
            log.info { "Created distribution track ${mutableSong.title} with track_id ${response.trackId}: ${response.message}" }
            mutableSong = mutableSong.copy(distributionTrackId = response.trackId)
            songRepository.update(mutableSong.id!!, mutableSong)
            trackFile.delete()

            // update track with collaborators and other metadata
            val updateTrackResponse = updateTrack(user, response.trackId, mutableSong)
            log.info { "Updated distribution track ${song.title} with track_id ${response.trackId}: ${updateTrackResponse.message}" }
        }

        val getTrackResponse = getTracks(user, mutableSong.distributionTrackId!!)
        val stereoIsrc = getTrackResponse.trackData!!.first().stereoIsrc
        require(stereoIsrc.length == 12) { "Invalid stereo isrc: $stereoIsrc" }
        if (mutableSong.isrc != stereoIsrc) {
            // Save the isrc to the song
            val isrcCountry = stereoIsrc.substring(0, 2)
            val isrcRegistrant = stereoIsrc.substring(2, 5)
            val isrcYear = stereoIsrc.substring(5, 7)
            val isrcDesignation = stereoIsrc.substring(7, 12)
            val isrc = "$isrcCountry-$isrcRegistrant-$isrcYear-$isrcDesignation"
            log.info { "Updating song isrc from ${mutableSong.isrc} to $isrc" }
            mutableSong = mutableSong.copy(isrc = isrc)
            songRepository.update(mutableSong.id!!, mutableSong)
        }

        // Wait for track to be processed
        withTimeout(10.minutes.inWholeMilliseconds) {
            while (true) {
                if (isTrackStatusCompleted(user, mutableSong.distributionTrackId!!)) {
                    break
                }
                delay(10000L)
            }
        }

        // Add or Update Distribution Album
        val getAlbumResponse = getAlbums(user)
        if (getAlbumResponse.totalRecords > 0) {
            val existingAlbum = getAlbumResponse.albumData.firstOrNull { it.name == mutableSong.title }
            if (existingAlbum == null) {
                val response = addAlbum(user, mutableSong.distributionTrackId!!, mutableSong)
                log.info { "Created distribution album ${mutableSong.title} with id ${response.releaseId}: ${response.message}" }
                val albumData = getAlbums(user).albumData.first { it.releaseId == response.releaseId }
                val barcodeType = albumData.productCodeType.toSongBarcodeType()
                val barcode = albumData.eanUpc
                mutableSong = mutableSong.copy(
                    distributionReleaseId = response.releaseId,
                    barcodeNumber = barcode,
                    barcodeType = barcodeType
                )
                songRepository.update(mutableSong.id!!, mutableSong)
            } else {
                log.info { "Found existing distribution album ${mutableSong.title} with id ${existingAlbum.releaseId}, title ${existingAlbum.name}" }
                if (mutableSong.distributionReleaseId == null) {
                    mutableSong = mutableSong.copy(distributionReleaseId = existingAlbum.releaseId)
                    songRepository.update(mutableSong.id!!, mutableSong)
                } else {
                    require(mutableSong.distributionReleaseId == existingAlbum.releaseId) { "Song.distributionReleaseId: ${mutableSong.distributionReleaseId} does not match existing distribution album! ${existingAlbum.releaseId}" }
                }
            }
        } else {
            val response = addAlbum(user, mutableSong.distributionTrackId!!, mutableSong)
            log.info { "Created distribution album ${mutableSong.title} with id ${response.releaseId}: ${response.message}" }
            // Get the album barcode
            val albumData = getAlbums(user).albumData.first { it.releaseId == response.releaseId }
            val barcodeType = albumData.productCodeType.toSongBarcodeType()
            val barcode = albumData.eanUpc
            mutableSong = mutableSong.copy(
                distributionReleaseId = response.releaseId,
                barcodeNumber = barcode,
                barcodeType = barcodeType
            )
            songRepository.update(mutableSong.id!!, mutableSong)
        }

        // Validate Distribution Album
        val validateReleaseResponse = validateAlbum(user, mutableSong.distributionReleaseId!!)
        require(validateReleaseResponse.validateData.errorFields.isNullOrEmpty()) { "Error validating release: $validateReleaseResponse" }
        log.info { "Validated distribution album ${mutableSong.title} with id ${mutableSong.distributionReleaseId}: ${validateReleaseResponse.message}" }

//        /**
//         * FIXME: Simulate distributed should only be done for testnet and AFTER we have initiated distribution.
//         */
//        // Simulate the release to outlets
//        val simulateReleaseResponse = simulateDistributeRelease(user, mutableSong.distributionReleaseId!!)
//        log.info { "Simulated release ${mutableSong.title} with id ${mutableSong.distributionReleaseId}: ${simulateReleaseResponse.message}" }

        // Distribute the release to outlets
        val distributeReleaseResponse =
            distributeReleaseToOutlets(user, mutableSong.releaseDate!!, mutableSong.distributionReleaseId!!)
        require(distributeReleaseResponse.releaseData?.errorFields.isNullOrEmpty()) { "Error distributing release: $distributeReleaseResponse" }
        log.info { "Distributed release ${mutableSong.title} with id ${mutableSong.distributionReleaseId} to outlets: ${distributeReleaseResponse.message}" }

        // Distribute the release to future outlets
        val distributeFutureReleaseResponse =
            distributeReleaseToFutureOutlets(user, mutableSong.distributionReleaseId!!)
        log.info { "Distributed release ${mutableSong.title} with id ${mutableSong.distributionReleaseId} to future outlets: ${distributeFutureReleaseResponse.message}" }
    }

    override suspend fun getEarliestReleaseDate(userId: UUID): LocalDate {
        val user = userRepository.get(userId)
        createDistributionUserIfNeeded(user)
        val maxDays = getOutlets(user).outlets.maxOfOrNull { it.processDurationDates }
            ?: throw HttpServiceUnavailableException("No Distribution Outlets available - retry later")
        return maxDays.toReleaseDate()
    }

    private suspend fun createDistributionUserIfNeeded(user: User) {
        // Create the distribution user if they don't yet exist
        val getUserResponse = getUser(user)
        if (getUserResponse.totalRecords > 0) {
            val existingUser = getUserResponse.users.first()
            log.info { "Found existing distribution user ${user.email} with id ${existingUser.uuid}" }
            if (user.distributionUserId == null) {
                user.distributionUserId = existingUser.uuid
                userRepository.updateUserData(user.id!!, user)
            } else {
                require(user.distributionUserId == existingUser.uuid) { "User.distributionUserId: ${user.distributionUserId} does not match existing distribution user! ${existingUser.uuid}" }
            }
            // validate that the user's information matches eveara
            if (existingUser.email != user.email ||
                existingUser.firstName != user.firstName?.ifBlank { "---" } ||
                existingUser.lastName != user.lastName?.ifBlank { "---" }
            ) {
                val response = updateUser(user)
                log.info { "Updated distribution user ${user.email} with id ${user.distributionUserId}: ${response.message}" }
            }
        } else {
            require(user.distributionUserId == null) { "User.distributionUserId: ${user.distributionUserId} not found in Eveara!" }
            val response = addUser(user)
            log.info { "Created distribution user ${user.email} with id ${response.uuid}: ${response.message}" }
            user.distributionUserId = response.uuid
            userRepository.updateUserData(user.id!!, user)
        }
    }

    private fun Long.toReleaseDate() = LocalDate.now().plusDays(this + 2)

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
