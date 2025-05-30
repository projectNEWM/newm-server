package io.newm.server.features.distribution.eveara

import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.retry
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
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.quote
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.utils.io.core.toByteArray
import io.ktor.utils.io.streams.asInput
import io.newm.chain.util.toB64String
import io.newm.server.client.QUALIFIER_UPLOAD_TRACK_HTTP_CLIENT
import io.newm.server.config.repo.ConfigRepository
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_EVEARA_CLIENT_ID
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_EVEARA_CLIENT_SECRET
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_EVEARA_PARTNER_SUBSCRIPTION_ID
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_EVEARA_SERVER
import io.newm.server.features.collaboration.model.Collaboration
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
import io.newm.server.features.distribution.model.EvearaErrorResponse
import io.newm.server.features.distribution.model.EvearaSimpleResponse
import io.newm.server.features.distribution.model.GetAlbumResponse
import io.newm.server.features.distribution.model.GetArtistResponse
import io.newm.server.features.distribution.model.GetCountriesResponse
import io.newm.server.features.distribution.model.GetGenresResponse
import io.newm.server.features.distribution.model.GetLanguagesResponse
import io.newm.server.features.distribution.model.GetOutletProfileNamesResponse
import io.newm.server.features.distribution.model.GetOutletsResponse
import io.newm.server.features.distribution.model.GetParticipantsResponse
import io.newm.server.features.distribution.model.GetPayoutBalanceResponse
import io.newm.server.features.distribution.model.GetPayoutHistoryResponse
import io.newm.server.features.distribution.model.GetRolesResponse
import io.newm.server.features.distribution.model.GetSmartLinksResponse
import io.newm.server.features.distribution.model.GetTrackStatusResponse
import io.newm.server.features.distribution.model.GetTracksResponse
import io.newm.server.features.distribution.model.GetUserLabelResponse
import io.newm.server.features.distribution.model.GetUserResponse
import io.newm.server.features.distribution.model.GetUserSubscriptionResponse
import io.newm.server.features.distribution.model.InitiatePayoutResponse
import io.newm.server.features.distribution.model.OutletProfile
import io.newm.server.features.distribution.model.OutletStatusCode
import io.newm.server.features.distribution.model.OutletsDetail
import io.newm.server.features.distribution.model.Participant
import io.newm.server.features.distribution.model.Preview
import io.newm.server.features.distribution.model.SmartLink
import io.newm.server.features.distribution.model.Subscription
import io.newm.server.features.distribution.model.Track
import io.newm.server.features.distribution.model.UpdateArtistRequest
import io.newm.server.features.distribution.model.UpdateArtistResponse
import io.newm.server.features.distribution.model.UpdateTrackRequest
import io.newm.server.features.distribution.model.UpdateUserLabelRequest
import io.newm.server.features.distribution.model.UpdateUserLabelResponse
import io.newm.server.features.distribution.model.UpdateUserRequest
import io.newm.server.features.distribution.model.ValidateAlbumResponse
import io.newm.server.features.song.model.MintingStatus
import io.newm.server.features.song.model.Release
import io.newm.server.features.song.model.Song
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
import io.newm.server.typealiases.UserId
import io.newm.shared.exception.HttpServiceUnavailableException
import io.newm.shared.koin.inject
import io.newm.shared.ktx.containsIgnoreCase
import io.newm.shared.ktx.getConfigString
import io.newm.shared.ktx.info
import io.newm.shared.ktx.orNull
import io.newm.shared.ktx.orZero
import java.io.File
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDate
import kotlin.random.Random.Default.nextLong
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.parameter.parametersOf
import org.slf4j.Logger
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.GetObjectRequest

class EvearaDistributionRepositoryImpl(
    private val collabRepository: CollaborationRepository,
    private val configRepository: ConfigRepository,
) : DistributionRepository {
    private val log: Logger by inject { parametersOf(javaClass.simpleName) }
    private val json: Json by inject()
    private val userRepository: UserRepository by inject()
    private val songRepository: SongRepository by inject()
    private val httpClient: HttpClient by inject()
    private val trackUploadHttpClient: HttpClient by inject(QUALIFIER_UPLOAD_TRACK_HTTP_CLIENT)
    private val s3AsyncClient: S3AsyncClient by inject()
    private val applicationEnvironment: ApplicationEnvironment by inject()
    private val evearaServer by lazy { applicationEnvironment.getConfigString(CONFIG_KEY_EVEARA_SERVER) }
    private val evearaApiBaseUrl by lazy {
        if (evearaServer == "api.eveara.com") {
            // mainnet
            "https://$evearaServer/v2.1"
        } else {
            // staging
            "https://$evearaServer/api/v2.1"
        }
    }

    private suspend fun evearaClientId() = applicationEnvironment.getSecureConfigString(CONFIG_KEY_EVEARA_CLIENT_ID)

    private suspend fun evearaClientSecret() = applicationEnvironment.getSecureConfigString(CONFIG_KEY_EVEARA_CLIENT_SECRET)

    private val getTokenMutex = Mutex()
    private val apiTokenCache =
        Caffeine
            .newBuilder()
            .expireAfterWrite(Duration.ofHours(1L))
            .build<Int, ApiToken>()

    private val rolesMutex = Mutex()
    private val rolesCache =
        Caffeine
            .newBuilder()
            .expireAfterWrite(Duration.ofDays(1L))
            .build<Int, GetRolesResponse>()

    private val genresMutex = Mutex()
    private val genresCache =
        Caffeine
            .newBuilder()
            .expireAfterWrite(Duration.ofDays(1L))
            .build<Int, GetGenresResponse>()

    private val languagesMutex = Mutex()
    private val languagesCache =
        Caffeine
            .newBuilder()
            .expireAfterWrite(Duration.ofDays(1L))
            .build<Int, GetLanguagesResponse>()

    private val countriesMutex = Mutex()
    private val countriesCache =
        Caffeine
            .newBuilder()
            .expireAfterWrite(Duration.ofDays(1L))
            .build<Int, GetCountriesResponse>()

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
                val response =
                    httpClient.post("$evearaApiBaseUrl/oauth/gettoken") {
                        retry {
                            maxRetries = 2
                            delayMillis { 500L }
                        }
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
                val apiToken =
                    ApiToken(
                        accessToken = apiTokenResponse.accessToken,
                        refreshToken = apiTokenResponse.refreshToken,
                        expiryTimestampMillis = System.currentTimeMillis() + apiTokenResponse.expiresInSeconds * 1000L
                    )
                apiTokenCache.put(-1, apiToken)

                apiToken.accessToken
            }
        }
    }

    override suspend fun getGenres(): GetGenresResponse =
        genresMutex.withLock {
            genresCache.getIfPresent(-1) ?: run {
                val response =
                    httpClient.get("$evearaApiBaseUrl/genres") {
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

    override suspend fun getRoles(): GetRolesResponse =
        rolesMutex.withLock {
            rolesCache.getIfPresent(-1) ?: run {
                val response =
                    httpClient.get("$evearaApiBaseUrl/roles") {
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

    override suspend fun getLanguages(): GetLanguagesResponse =
        languagesMutex.withLock {
            languagesCache.getIfPresent(-1) ?: run {
                val response =
                    httpClient.get("$evearaApiBaseUrl/languages") {
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

    override suspend fun getCountries(): GetCountriesResponse =
        countriesMutex.withLock {
            countriesCache.getIfPresent(-1) ?: run {
                val response =
                    httpClient.get("$evearaApiBaseUrl/countries") {
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

    override suspend fun getOutlets(user: User): GetOutletsResponse {
        val response =
            httpClient.get("$evearaApiBaseUrl/outlets") {
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
        return getOutletsResponse
    }

    override suspend fun addUser(user: User): AddUserResponse {
        requireNotNull(user.firstName) { "User.firstName must not be null!" }
        requireNotNull(user.lastName) { "User.lastName must not be null!" }
        requireNotNull(user.email) { "User.email must not be null!" }
        val response =
            httpClient.post("$evearaApiBaseUrl/users") {
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
        val response =
            httpClient.get("$evearaApiBaseUrl/users") {
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
        val response =
            httpClient.put("$evearaApiBaseUrl/users") {
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
        val response =
            httpClient.post("$evearaApiBaseUrl/subscriptions") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                bearerAuth(getEvearaApiToken())
                setBody(
                    AddUserSubscriptionRequest(
                        uuid = user.distributionUserId!!,
                        subscriptions =
                            listOf(
                                Subscription(
                                    subscriptionId = configRepository.getLong(CONFIG_KEY_EVEARA_PARTNER_SUBSCRIPTION_ID),
                                    // Add random number so no dups on eveara side
                                    partnerReferenceId = "${user.id}_${nextLong(Long.MAX_VALUE)}".substring(0..49),
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
        val response =
            httpClient.get("$evearaApiBaseUrl/subscriptions") {
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

    override suspend fun addUserLabel(
        distributionUserId: String,
        label: String
    ): AddUserLabelResponse {
        val response =
            httpClient.post("$evearaApiBaseUrl/labels") {
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
        val response =
            httpClient.put("$evearaApiBaseUrl/labels/$distributionLabelId") {
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
        val response =
            httpClient.get("$evearaApiBaseUrl/labels") {
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
        val response =
            httpClient.delete("$evearaApiBaseUrl/labels/${user.distributionLabelId}") {
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
        val response =
            httpClient.post("$evearaApiBaseUrl/artists") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                bearerAuth(getEvearaApiToken())
                setBody(addArtistRequest.logRequestJson(log))
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

    override suspend fun updateArtist(
        artistId: Long,
        updateArtistRequest: UpdateArtistRequest
    ): UpdateArtistResponse {
        val response =
            httpClient.put("$evearaApiBaseUrl/artists/$artistId") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                bearerAuth(getEvearaApiToken())
                setBody(updateArtistRequest.logRequestJson(log))
            }
        if (!response.status.isSuccess()) {
            throw ServerResponseException(response, "Error updating artist!: ${response.bodyAsText()}")
        }
        val updateArtistResponse: UpdateArtistResponse = response.body()
        if (!updateArtistResponse.success) {
            throw ServerResponseException(response, "Error updating artist! success==false")
        }

        return updateArtistResponse
    }

    override suspend fun getArtist(user: User): GetArtistResponse {
        val response =
            httpClient.get("$evearaApiBaseUrl/artists/${user.distributionArtistId}") {
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
        val response =
            httpClient.get("$evearaApiBaseUrl/artists") {
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

    override suspend fun getArtistOutletProfileNames(user: User): GetOutletProfileNamesResponse {
        requireNotNull(user.distributionUserId) { "User.distributionUserId must not be null!" }
        val response =
            httpClient.get("$evearaApiBaseUrl/artist/outlet-profile") {
                accept(ContentType.Application.Json)
                bearerAuth(getEvearaApiToken())
                parameter("uuid", user.distributionUserId)
            }
        if (!response.status.isSuccess()) {
            throw ServerResponseException(response, "Error getting artist outlet profiles!: ${response.bodyAsText()}")
        }
        val getOutletProfileNamesResponse: GetOutletProfileNamesResponse = response.body()
        if (!getOutletProfileNamesResponse.success) {
            throw ServerResponseException(response, "Error getting artist outlet profiles! success==false")
        }

        return getOutletProfileNamesResponse
    }

    override suspend fun addParticipant(
        user: User,
        collabUser: User?,
        collab: Collaboration?
    ): AddParticipantResponse {
        requireNotNull(user.distributionUserId) { "User.distributionUserId must not be null!" }
        if (collabUser == null) {
            if (user.distributionParticipantId != null) {
                return AddParticipantResponse(
                    success = true,
                    message = "User already has a participantId!",
                    participantId = user.distributionParticipantId!!
                )
            }
        } else {
            requireNotNull(collab) { "Collaboration must not be null!" }
            if (collab.distributionParticipantId != null) {
                return AddParticipantResponse(
                    success = true,
                    message = "User already has a participantId!",
                    participantId = collab.distributionParticipantId
                )
            }
        }
        val response =
            httpClient.post("$evearaApiBaseUrl/participants") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                bearerAuth(getEvearaApiToken())
                setBody(
                    AddParticipantRequest(
                        uuid = user.distributionUserId!!,
                        name = collabUser?.stageOrFullName ?: user.stageOrFullName,
                        isni = collabUser?.distributionIsni ?: user.distributionIsni,
                        ipn = collabUser?.distributionIpn ?: user.distributionIpn,
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

    override suspend fun updateParticipant(
        user: User,
        collabUser: User?,
        collab: Collaboration?
    ): EvearaSimpleResponse {
        if (collabUser == null) {
            requireNotNull(user.distributionParticipantId) { "User.distributionParticipantId must not be null!" }
        } else {
            requireNotNull(collab) { "Collaboration must not be null!" }
            requireNotNull(collab.distributionParticipantId) { "Collaboration.distributionParticipantId must not be null!" }
        }
        val response =
            httpClient.put("$evearaApiBaseUrl/participants/${user.distributionParticipantId}") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                bearerAuth(getEvearaApiToken())
                setBody(
                    AddParticipantRequest(
                        uuid = user.distributionUserId!!,
                        name = collabUser?.stageOrFullName ?: user.stageOrFullName,
                        isni = collabUser?.distributionIsni ?: user.distributionIsni,
                        ipn = collabUser?.distributionIpn ?: user.distributionIpn,
                    ).logRequestJson(log)
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
        val response =
            httpClient.get("$evearaApiBaseUrl/participants") {
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

    override suspend fun addTrack(
        user: User,
        trackFile: File
    ): AddTrackResponse {
        requireNotNull(user.distributionUserId) { "User.distributionUserId must not be null!" }

        val response =
            trackUploadHttpClient.submitFormWithBinaryData(
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

    override suspend fun updateTrack(
        user: User,
        trackId: Long,
        song: Song
    ): EvearaSimpleResponse {
        requireNotNull(user.distributionUserId) { "User.distributionUserId must not be null!" }
        val genres = getGenres().genres
        val languages = getLanguages().languages
        val collabs = collabRepository.getAllBySongId(song.id!!)
        val artistIds = collectSongArtistIdsList(user, collabs)
        val featuredArtistIds = collectFeaturedArtistIdsList(user, collabs)

        val response =
            httpClient.put("$evearaApiBaseUrl/tracks/$trackId") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                bearerAuth(getEvearaApiToken())
                setBody(
                    UpdateTrackRequest(
                        uuid = user.distributionUserId!!,
                        name = song.title!!,
                        // Eveara API expects ISRC/ISWC without dashes
                        stereoIsrc = song.isrc?.replace("-", ""),
                        iswc = song.iswc?.replace("-", ""),
                        genre = song.genres?.mapNotNull { songGenreName -> genres.firstOrNull { it.name == songGenreName }?.genreId },
                        language = languages.find { it.name == song.language }?.code,
                        explicit =
                            if (song.parentalAdvisory.equals("Non-Explicit", ignoreCase = true)) {
                                // Clean
                                null
                            } else {
                                // Explicit
                                1
                            },
                        // 1 = Download, 2 = Streaming
                        availability = listOf(1, 2),
                        artists = artistIds,
                        featuredArtists = featuredArtistIds,
                        albumOnly = false,
                        // TODO: Implement lyrics later.
                        lyrics = null,
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

    override suspend fun getTracks(
        user: User,
        trackId: Long?
    ): GetTracksResponse {
        requireNotNull(user.distributionUserId) { "User.distributionUserId must not be null!" }
        val response =
            httpClient.get("$evearaApiBaseUrl/tracks${trackId?.let { "/$trackId" } ?: ""}") {
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

    override suspend fun deleteTrack(
        user: User,
        trackId: Long
    ): EvearaSimpleResponse {
        requireNotNull(user.distributionUserId) { "User.distributionUserId must not be null!" }
        val response =
            httpClient.delete("$evearaApiBaseUrl/tracks/$trackId") {
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

    override suspend fun isTrackStatusCompleted(
        user: User,
        trackId: Long
    ): Boolean {
        val response =
            httpClient.get("$evearaApiBaseUrl/tracks/$trackId/status") {
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

        log.info { "Track status for user ${user.distributionUserId} with trackId $trackId: ${getTrackStatusResponse.trackStatus.stereo}" }

        return getTrackStatusResponse.trackStatus.stereo?.equals("Completed", ignoreCase = true) ?: false
    }

    override suspend fun addAlbum(
        user: User,
        release: Release,
        songs: List<Song>
    ): AddAlbumResponse {
        requireNotNull(user.distributionUserId) { "User.distributionUserId must not be null!" }
        val releaseArtistIds = mutableSetOf<Long>()
        val tracks =
            songs.map { song ->
                requireNotNull(song.id) { "Song.id must not be null!" }
                requireNotNull(song.distributionTrackId) { "Song.distributionTrackId must not be null!" }
                val collabs = collabRepository.getAllBySongId(song.id)
                val artistIds = collectSongArtistIdsList(user, collabs)
                releaseArtistIds.addAll(artistIds)
                val featuredArtistIds = collectFeaturedArtistIdsList(user, collabs)
                val participants = collectSongParticipantsList(user, collabs)

                Track(
                    trackId = song.distributionTrackId,
                    artistIds = artistIds,
                    featuredArtists = featuredArtistIds,
                    // TODO: Allow user to specify this later
                    preview = Preview(startAt = 0, duration = 30),
                    participants = participants,
                    instrumental =
                        song.instrumental ?: song.genres?.any {
                            it.equals(
                                "Instrumental",
                                ignoreCase = true
                            )
                        } ?: false,
                )
            }
        val response =
            httpClient.post("$evearaApiBaseUrl/albums") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                bearerAuth(getEvearaApiToken())
                setBody(
                    AddAlbumRequest(
                        uuid = user.distributionUserId!!,
                        name = release.title,
                        artistIds = releaseArtistIds.toList(),
                        subscriptionId = user.distributionSubscriptionId!!,
                        eanUpc = release.barcodeNumber,
                        productCodeType = release.releaseProductCodeType,
                        labelId = user.distributionLabelId,
                        productType = release.releaseType!!.name.lowercase(),
                        codeAutoGenerate = release.barcodeNumber == null,
                        productFormat = "stereo",
                        coverImage =
                            CoverImage(
                                url = release.coverArtUrl.asValidUrl(),
                                extension =
                                    release.coverArtUrl
                                        .asValidUrl()
                                        .substringAfterLast(".")
                                        .lowercase(),
                            ),
                        originalReleaseDate = release.releaseDate,
                        tracks = tracks,
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
        val response =
            httpClient.get("$evearaApiBaseUrl/albums") {
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

    override suspend fun updateAlbum(
        user: User,
        release: Release,
        songs: List<Song>
    ): EvearaSimpleResponse {
        requireNotNull(user.distributionUserId) { "User.distributionUserId must not be null!" }
        requireNotNull(release.distributionReleaseId) { "Song.distributionReleaseId must not be null!" }
        val releaseArtistIds = mutableSetOf<Long>()
        val tracks =
            songs.map { song ->
                requireNotNull(song.id) { "Song.id must not be null!" }
                requireNotNull(song.distributionTrackId) { "Song.distributionTrackId must not be null!" }
                val collabs = collabRepository.getAllBySongId(song.id)
                val artistIds = collectSongArtistIdsList(user, collabs)
                releaseArtistIds.addAll(artistIds)
                val featuredArtistIds = collectFeaturedArtistIdsList(user, collabs)
                val participants = collectSongParticipantsList(user, collabs)

                Track(
                    trackId = song.distributionTrackId,
                    artistIds = artistIds,
                    featuredArtists = featuredArtistIds,
                    // TODO: Allow user to specify this later
                    preview = Preview(startAt = 0, duration = 30),
                    participants = participants,
                    instrumental =
                        song.instrumental ?: song.genres?.any {
                            it.equals(
                                "Instrumental",
                                ignoreCase = true
                            )
                        } ?: false,
                )
            }
        val response =
            httpClient.put("$evearaApiBaseUrl/albums/${release.distributionReleaseId}") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                bearerAuth(getEvearaApiToken())
                setBody(
                    AddAlbumRequest(
                        uuid = user.distributionUserId!!,
                        name = release.title,
                        artistIds = releaseArtistIds.toList(),
                        subscriptionId = user.distributionSubscriptionId!!,
                        eanUpc = release.barcodeNumber,
                        productCodeType = release.releaseProductCodeType,
                        labelId = user.distributionLabelId,
                        productType = release.releaseType!!.name.lowercase(),
                        codeAutoGenerate = release.barcodeNumber == null,
                        productFormat = "stereo",
                        coverImage =
                            CoverImage(
                                url = release.coverArtUrl.asValidUrl(),
                                extension =
                                    release.coverArtUrl
                                        .asValidUrl()
                                        .substringAfterLast(".")
                                        .lowercase(),
                            ),
                        originalReleaseDate = release.releaseDate,
                        tracks = tracks,
                    ).logRequestJson(log)
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

    override suspend fun validateAlbum(
        user: User,
        releaseId: Long
    ): ValidateAlbumResponse {
        requireNotNull(user.distributionUserId) { "User.distributionUserId must not be null!" }
        val response =
            httpClient.get("$evearaApiBaseUrl/albums/$releaseId/validate") {
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

    override suspend fun deleteAlbum(
        user: User,
        releaseId: Long
    ): EvearaSimpleResponse {
        requireNotNull(user.distributionUserId) { "User.distributionUserId must not be null!" }
        val response =
            httpClient.delete("$evearaApiBaseUrl/albums/$releaseId") {
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

    override suspend fun simulateDistributeRelease(
        user: User,
        releaseId: Long
    ): EvearaSimpleResponse {
        requireNotNull(user.distributionUserId) { "User.distributionUserId must not be null!" }
        val response =
            httpClient.put("$evearaApiBaseUrl/simulate/distribute") {
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
        release: Release,
        allowRetry: Boolean,
    ): DistributeReleaseResponse {
        requireNotNull(user.distributionUserId) { "User.distributionUserId must not be null!" }
        requireNotNull(release.distributionReleaseId) { "Release.distributionReleaseId must not be null!" }
        requireNotNull(release.releaseDate) { "Release.releaseDate must not be null!" }
        val response =
            httpClient.patch("$evearaApiBaseUrl/outlets/${release.distributionReleaseId}/distribute") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                bearerAuth(getEvearaApiToken())
                setBody(
                    DistributeReleaseRequest(
                        uuid = user.distributionUserId!!,
                        outletsDetails =
                            getOutlets(user).outlets.map { evearaOutlet ->
                                OutletsDetail(
                                    storeId = evearaOutlet.storeId,
                                    releaseStartDate = release.releaseDate,
                                )
                            }
                    ).logRequestJson(log)
                )
            }
        if (!response.status.isSuccess()) {
            val bodyAsString = response.bodyAsText()
            if (allowRetry) {
                try {
                    json.decodeFromString<EvearaErrorResponse>(bodyAsString)
                } catch (e: Throwable) {
                    log.error("Error parsing response body as EvearaErrorResponse: $bodyAsString", e)
                    null
                }?.let { evearaErrorResponse ->
                    if (evearaErrorResponse.errors?.find { it.code == 8018 } != null) {
                        // invalid release start date. Try it again one more time with a different date
                        val earliestReleaseDate = getEarliestReleaseDate(user.id!!)
                        val updatedRelease = release.copy(releaseDate = earliestReleaseDate)
                        songRepository.update(release.id!!, Release(releaseDate = earliestReleaseDate))
                        // retry it once
                        return distributeReleaseToOutlets(user, updatedRelease, false)
                    }
                }
            }
            throw ServerResponseException(response, "Error distributing release to outlets!: $bodyAsString")
        }
        val distributeReleaseResponse: DistributeReleaseResponse = response.body()
        if (!distributeReleaseResponse.success) {
            throw ServerResponseException(response, "Error distributing release to outlets! success==false")
        }

        return distributeReleaseResponse
    }

    override suspend fun distributeReleaseToFutureOutlets(
        user: User,
        releaseId: Long
    ): DistributeReleaseResponse {
        requireNotNull(user.distributionUserId) { "User.distributionUserId must not be null!" }
        val response =
            httpClient.patch("$evearaApiBaseUrl/outlets/$releaseId/future-outlets") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                bearerAuth(getEvearaApiToken())
                setBody(
                    DistributeFutureReleaseRequest(
                        uuid = user.distributionUserId!!,
                        enableDistributeToFutureOutlets = true,
                    ).logRequestJson(log)
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
        val response =
            httpClient.get("$evearaApiBaseUrl/outlets/$releaseId") {
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

    override suspend fun distributeRelease(release: Release) {
        requireNotNull(release.id) { "Release.id must not be null!" }
        requireNotNull(release.ownerId) { "Release.ownerId must not be null!" }
        val releaseOwner = userRepository.get(release.ownerId)

        // Create the distribution user if they don't exist yet
        createDistributionUserIfNeeded(releaseOwner)

        // Create the distribution subscription if it doesn't yet exist
        createDistributionSubscription(releaseOwner)

        requireNotNull(releaseOwner.id) { "User.id must not be null!" }
        val songs = songRepository.getAllByReleaseId(release.id)

        if (songs.size > 1) {
            require(songs.all { it.track != null }) { "All songs must have a track number for ep/album/compilation!" }
        }

        val uploadedSongs =
            songs.sortedBy { it.track ?: 0 }.map { song ->
                // Create the distribution artistId for each collaborator under this user account if they don't exist yet
                val collabs = collabRepository.getAllBySongId(song.id!!)
                createDistributionArtistsForCollabs(song, releaseOwner, collabs)

                // Create the distribution participants if they don't exist yet
                createDistributionParticipants(releaseOwner, collabs)

                // Add or Update Distribution Label
                createDistributionLabel(releaseOwner, song)

                // Add or Update Distribution Track
                createDistributionTrack(releaseOwner, song)
            }

        // Add or Update Distribution Album
        var mutableRelease = createDistributionAlbum(releaseOwner, release, uploadedSongs)

        // Validate Distribution Album
        val validateReleaseResponse = validateAlbum(releaseOwner, mutableRelease.distributionReleaseId!!)
        require(validateReleaseResponse.validateData.errorFields.isNullOrEmpty()) { "Error validating release: $validateReleaseResponse" }
        log.info {
            "Validated distribution album ${mutableRelease.title} with id ${mutableRelease.distributionReleaseId}: ${validateReleaseResponse.message}"
        }

        mutableRelease = distributeAlbumRelease(releaseOwner, mutableRelease)

        // Mark the release as having been submitted for distribution the first time. This helps us ensure we don't allow
        // certain things like release date to be edited when we have to re-attempt distribution for the song.
        songRepository.update(mutableRelease.id!!, Release(hasSubmittedForDistribution = true))
    }

    override suspend fun redistributeRelease(release: Release) {
        requireNotNull(release.id) { "Release.id must not be null!" }
        requireNotNull(release.ownerId) { "Release.ownerId must not be null!" }
        val user = userRepository.get(release.ownerId)
        requireNotNull(user.id) { "User.id must not be null!" }

        val distributionReleaseStatusResponse = distributionOutletReleaseStatus(user, release.distributionReleaseId!!)
        val spotifyOutletStatusCode =
            distributionReleaseStatusResponse.outletReleaseStatuses
                ?.find {
                    it.storeName.equals(
                        "Spotify",
                        ignoreCase = true
                    )
                }?.outletStatus
                ?.statusCode

        require(spotifyOutletStatusCode == OutletStatusCode.DISAPPROVED) {
            "Song must be in a disapproved status to redistribute!"
        }
        // Check a second time that they actually paid for the minting and then re-distribute
        songRepository.getAllByReleaseId(release.id).first().let { song ->
            songRepository.updateSongMintingStatus(song.id!!, MintingStatus.MintingPaymentSubmitted)
        }
    }

    override suspend fun getEarliestReleaseDate(userId: UserId): LocalDate {
        val user = userRepository.get(userId)
        createDistributionUserIfNeeded(user)
        createDistributionSubscription(user)
        val maxDays =
            getOutlets(user).outlets.maxOfOrNull { it.processDurationDates }
                ?: throw HttpServiceUnavailableException("No Distribution Outlets available - retry later")
        return maxDays.toReleaseDate()
    }

    override suspend fun getPayoutBalance(userId: UserId): GetPayoutBalanceResponse {
        val user = userRepository.get(userId)
        requireNotNull(user.distributionUserId) { "User.distributionUserId must not be null!" }
        val response =
            httpClient.get("$evearaApiBaseUrl/payout/${user.distributionUserId}/balance") {
                accept(ContentType.Application.Json)
                bearerAuth(getEvearaApiToken())
            }
        if (!response.status.isSuccess()) {
            throw ServerResponseException(response, "Error getting payout balance!: ${response.bodyAsText()}")
        }
        val getPayoutBalanceResponse: GetPayoutBalanceResponse = response.body()
        if (!getPayoutBalanceResponse.success) {
            throw ServerResponseException(response, "Error getting payout balance! success==false")
        }

        return getPayoutBalanceResponse
    }

    override suspend fun getPayoutHistory(userId: UserId): GetPayoutHistoryResponse {
        val user = userRepository.get(userId)
        requireNotNull(user.distributionUserId) { "User.distributionUserId must not be null!" }
        val response =
            httpClient.get("$evearaApiBaseUrl/payout/${user.distributionUserId}/history") {
                accept(ContentType.Application.Json)
                bearerAuth(getEvearaApiToken())
            }
        if (!response.status.isSuccess()) {
            throw ServerResponseException(response, "Error getting payout history!: ${response.bodyAsText()}")
        }
        val getPayoutHistoryResponse: GetPayoutHistoryResponse = response.body()
        if (!getPayoutHistoryResponse.success) {
            throw ServerResponseException(response, "Error getting payout history! success==false")
        }

        return getPayoutHistoryResponse
    }

    override suspend fun initiatePayout(userId: UserId): InitiatePayoutResponse {
        val user = userRepository.get(userId)
        requireNotNull(user.distributionUserId) { "User.distributionUserId must not be null!" }
        val response =
            httpClient.post("$evearaApiBaseUrl/payout/${user.distributionUserId}") {
                accept(ContentType.Application.Json)
                bearerAuth(getEvearaApiToken())
            }
        if (!response.status.isSuccess()) {
            throw ServerResponseException(response, "Error initiating payout!: ${response.bodyAsText()}")
        }
        val initiatePayoutResponse: InitiatePayoutResponse = response.body()
        if (!initiatePayoutResponse.success) {
            throw ServerResponseException(response, "Error initiating payout! success==false")
        }

        return initiatePayoutResponse
    }

    override suspend fun createDistributionUserIfNeeded(user: User) {
        // Create the distribution user if they don't yet exist
        val getUserResponse = getUser(user)
        if (getUserResponse.totalRecords > 0) {
            val existingDistributionUser = getUserResponse.users.first()
            log.info { "Found existing distribution user ${user.email} with id ${existingDistributionUser.uuid}" }
            if (user.distributionUserId == null) {
                user.distributionUserId = existingDistributionUser.uuid
                userRepository.updateUserData(user.id!!, user)
            } else {
                require(user.distributionUserId == existingDistributionUser.uuid) {
                    "User.distributionUserId: ${user.distributionUserId} does not match existing distribution user! ${existingDistributionUser.uuid}"
                }
            }
            // validate that the user's information matches eveara
            if (existingDistributionUser.email != user.email ||
                existingDistributionUser.firstName != user.firstName?.ifBlank { "---" } ||
                existingDistributionUser.lastName != user.lastName?.ifBlank { "---" }
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

    private suspend fun createDistributionArtistsForCollabs(
        song: Song,
        user: User,
        collabs: List<Collaboration>
    ) {
        val filteredCollars =
            collabs.filter { it.royaltyRate.orZero() > BigDecimal.ZERO }
        require(
            filteredCollars.all {
                it.status == CollaborationStatus.Accepted
            }
        ) { "All Collaborations must be accepted!" }

        // if genre contains classical, there must be at least one collaborator with a role of "Composer"
        if (song.genres?.containsIgnoreCase("Classical") == true) {
            require(
                collabs.any { it.roles?.containsIgnoreCase("Composer") == true }
            ) { "At least one collaborator must have a role of 'Composer' for classical genre!" }
        }

        val collabDistributionArtists = getArtists(user).artists
        val collabDistributionArtistsMap = collabDistributionArtists.associateBy { it.artistId }
        val collabDistributionArtistsNameToIdMap =
            collabDistributionArtists.associate { it.name to it.artistId }.toMutableMap()
        val collabUserOutletProfileNamesMap =
            getArtistOutletProfileNames(user).outletProfileNames.associate { it.name to it.id }
        collabs.forEach { collab ->
            if (collab.distributionArtistId == null) {
                val collabUser = userRepository.findByEmail(collab.email!!)

                collabDistributionArtistsNameToIdMap[collabUser.stageOrFullName]?.let { distributionArtistId ->
                    log.info { "Found existing collab distribution artist ${collabUser.email} with id $distributionArtistId" }
                    collabRepository.update(
                        collab.copy(distributionArtistId = distributionArtistId),
                        collab.id!!,
                        user.id!!,
                        skipStatusCheck = true
                    )

                    val currentOutletsMap =
                        collabDistributionArtistsMap[distributionArtistId]
                            ?.outlets
                            ?.filter { it.profileUrl.isNotBlank() }
                            ?.associateBy { it.name }
                            .orEmpty()
                    if (currentOutletsMap["Spotify"]?.profileUrl != collabUser.spotifyProfile?.orNull() ||
                        currentOutletsMap["SoundCloud"]?.profileUrl != collabUser.soundCloudProfile?.orNull() ||
                        currentOutletsMap["Apple"]?.profileUrl != collabUser.appleMusicProfile?.orNull()
                    ) {
                        val response =
                            updateArtist(
                                distributionArtistId,
                                UpdateArtistRequest(
                                    uuid = user.distributionUserId!!,
                                    name = collabUser.stageOrFullName,
                                    outletProfiles =
                                        listOf(
                                            OutletProfile(
                                                id = collabUserOutletProfileNamesMap["Spotify"]!!,
                                                profileUrl = collabUser.spotifyProfile.orEmpty(),
                                            ),
                                            OutletProfile(
                                                id = collabUserOutletProfileNamesMap["SoundCloud"]!!,
                                                profileUrl = collabUser.soundCloudProfile.orEmpty(),
                                            ),
                                            OutletProfile(
                                                id = collabUserOutletProfileNamesMap["Apple"]!!,
                                                profileUrl = collabUser.appleMusicProfile.orEmpty(),
                                            )
                                        ).filter { it.profileUrl.isNotBlank() }.takeIf { it.isNotEmpty() },
                                )
                            ).logRequestJson(log)
                        log.info {
                            "Updated collab distribution artist ${collabUser.email} with id ${response.artistData?.artistId}: ${response.message}"
                        }
                    }
                } ?: run {
                    val countryCode = getUserCountryCode(user)
                    val response =
                        addArtist(
                            AddArtistRequest(
                                uuid = user.distributionUserId!!,
                                name = collabUser.stageOrFullName,
                                country = countryCode,
                                outletProfiles =
                                    listOf(
                                        OutletProfile(
                                            id = collabUserOutletProfileNamesMap["Spotify"]!!,
                                            profileUrl = collabUser.spotifyProfile.orEmpty(),
                                        ),
                                        OutletProfile(
                                            id = collabUserOutletProfileNamesMap["SoundCloud"]!!,
                                            profileUrl = collabUser.soundCloudProfile.orEmpty(),
                                        ),
                                        OutletProfile(
                                            id = collabUserOutletProfileNamesMap["Apple"]!!,
                                            profileUrl = collabUser.appleMusicProfile.orEmpty(),
                                        )
                                    ).filter { it.profileUrl.isNotBlank() }.takeIf { it.isNotEmpty() },
                            )
                        ).logRequestJson(log)
                    log.info { "Created collab distribution artist ${collabUser.email} with id ${response.artistId}: ${response.message}" }
                    collabRepository.update(
                        collab.copy(distributionArtistId = response.artistId),
                        collab.id!!,
                        user.id!!,
                        skipStatusCheck = true
                    )
                    collabDistributionArtistsNameToIdMap[collabUser.stageOrFullName] = response.artistId
                }
            }
        }

        // Add Distribution Artist record for user account if they don't yet exist
        val existingDistributionArtist = collabDistributionArtistsNameToIdMap[user.stageOrFullName]
        if (user.distributionArtistId == null) {
            if (existingDistributionArtist != null) {
                log.info { "Found existing distribution artist ${user.email} with id $existingDistributionArtist" }
                user.distributionArtistId = existingDistributionArtist
                userRepository.updateUserData(user.id!!, user)

                val currentOutletsMap =
                    collabDistributionArtistsMap[user.distributionArtistId]
                        ?.outlets
                        ?.filter { it.profileUrl.isNotBlank() }
                        ?.associateBy { it.name }
                        .orEmpty()
                if (currentOutletsMap["Spotify"]?.profileUrl != user.spotifyProfile?.orNull() ||
                    currentOutletsMap["SoundCloud"]?.profileUrl != user.soundCloudProfile?.orNull() ||
                    currentOutletsMap["Apple"]?.profileUrl != user.appleMusicProfile?.orNull()
                ) {
                    val response =
                        updateArtist(
                            user.distributionArtistId!!,
                            UpdateArtistRequest(
                                uuid = user.distributionUserId!!,
                                name = user.stageOrFullName,
                                outletProfiles =
                                    listOf(
                                        OutletProfile(
                                            id = collabUserOutletProfileNamesMap["Spotify"]!!,
                                            profileUrl = user.spotifyProfile.orEmpty(),
                                        ),
                                        OutletProfile(
                                            id = collabUserOutletProfileNamesMap["SoundCloud"]!!,
                                            profileUrl = user.soundCloudProfile.orEmpty(),
                                        ),
                                        OutletProfile(
                                            id = collabUserOutletProfileNamesMap["Apple"]!!,
                                            profileUrl = user.appleMusicProfile.orEmpty(),
                                        )
                                    ).filter { it.profileUrl.isNotBlank() }.takeIf { it.isNotEmpty() },
                            )
                        ).logRequestJson(log)
                    log.info { "Updated distribution artist ${user.email} with id ${response.artistData?.artistId}: ${response.message}" }
                }
            } else {
                val artist = getArtists(user).artists.firstOrNull()
                if (artist == null) {
                    val countryCode = getUserCountryCode(user)
                    val response =
                        addArtist(
                            AddArtistRequest(
                                uuid = user.distributionUserId!!,
                                name = user.stageOrFullName,
                                country = countryCode,
                                outletProfiles =
                                    listOf(
                                        OutletProfile(
                                            id = collabUserOutletProfileNamesMap["Spotify"]!!,
                                            profileUrl = user.spotifyProfile.orEmpty(),
                                        ),
                                        OutletProfile(
                                            id = collabUserOutletProfileNamesMap["SoundCloud"]!!,
                                            profileUrl = user.soundCloudProfile.orEmpty(),
                                        ),
                                        OutletProfile(
                                            id = collabUserOutletProfileNamesMap["Apple"]!!,
                                            profileUrl = user.appleMusicProfile.orEmpty(),
                                        )
                                    ).filter { it.profileUrl.isNotBlank() }.takeIf { it.isNotEmpty() },
                            )
                        ).logRequestJson(log)
                    log.info { "Created distribution artist ${user.email} with id ${response.artistId}: ${response.message}" }
                    user.distributionArtistId = response.artistId
                    userRepository.updateUserData(user.id!!, user)
                } else {
                    log.info { "Found existing distribution artist ${user.email} with id ${artist.artistId}" }
                    user.distributionArtistId = artist.artistId
                    userRepository.updateUserData(user.id!!, user)

                    val currentOutletsMap =
                        collabDistributionArtistsMap[user.distributionArtistId]
                            ?.outlets
                            ?.filter { it.profileUrl.isNotBlank() }
                            ?.associateBy { it.name }
                            .orEmpty()
                    if (currentOutletsMap["Spotify"]?.profileUrl != user.spotifyProfile?.orNull() ||
                        currentOutletsMap["SoundCloud"]?.profileUrl != user.soundCloudProfile?.orNull() ||
                        currentOutletsMap["Apple"]?.profileUrl != user.appleMusicProfile?.orNull()
                    ) {
                        val response =
                            updateArtist(
                                user.distributionArtistId!!,
                                UpdateArtistRequest(
                                    uuid = user.distributionUserId!!,
                                    name = user.stageOrFullName,
                                    outletProfiles =
                                        listOf(
                                            OutletProfile(
                                                id = collabUserOutletProfileNamesMap["Spotify"]!!,
                                                profileUrl = user.spotifyProfile.orEmpty(),
                                            ),
                                            OutletProfile(
                                                id = collabUserOutletProfileNamesMap["SoundCloud"]!!,
                                                profileUrl = user.soundCloudProfile.orEmpty(),
                                            ),
                                            OutletProfile(
                                                id = collabUserOutletProfileNamesMap["Apple"]!!,
                                                profileUrl = user.appleMusicProfile.orEmpty(),
                                            )
                                        ).filter { it.profileUrl.isNotBlank() }.takeIf { it.isNotEmpty() },
                                )
                            ).logRequestJson(log)
                        log.info {
                            "Updated distribution artist ${user.email} with id ${response.artistData?.artistId}: ${response.message}"
                        }
                    }
                }
            }
        } else {
            val artist = getArtist(user).artists.first()
            log.info { "Found existing distribution artist ${user.email} with id ${artist.artistId}" }

            val currentOutletsMap =
                collabDistributionArtistsMap[user.distributionArtistId]
                    ?.outlets
                    ?.filter { it.profileUrl.isNotBlank() }
                    ?.associateBy { it.name }
                    .orEmpty()
            if (currentOutletsMap["Spotify"]?.profileUrl != user.spotifyProfile?.orNull() ||
                currentOutletsMap["SoundCloud"]?.profileUrl != user.soundCloudProfile?.orNull() ||
                currentOutletsMap["Apple"]?.profileUrl != user.appleMusicProfile?.orNull()
            ) {
                val response =
                    updateArtist(
                        user.distributionArtistId!!,
                        UpdateArtistRequest(
                            uuid = user.distributionUserId!!,
                            name = user.stageOrFullName,
                            outletProfiles =
                                listOf(
                                    OutletProfile(
                                        id = collabUserOutletProfileNamesMap["Spotify"]!!,
                                        profileUrl = user.spotifyProfile.orEmpty(),
                                    ),
                                    OutletProfile(
                                        id = collabUserOutletProfileNamesMap["SoundCloud"]!!,
                                        profileUrl = user.soundCloudProfile.orEmpty(),
                                    ),
                                    OutletProfile(
                                        id = collabUserOutletProfileNamesMap["Apple"]!!,
                                        profileUrl = user.appleMusicProfile.orEmpty(),
                                    )
                                ).filter { it.profileUrl.isNotBlank() }.takeIf { it.isNotEmpty() },
                        )
                    ).logRequestJson(log)
                log.info { "Updated distribution artist ${user.email} with id ${response.artistData?.artistId}: ${response.message}" }
            }
        }
    }

    override suspend fun createDistributionSubscription(user: User) {
        val getSubscriptionResponse = getUserSubscription(user)
        if (getSubscriptionResponse.totalRecords > 0) {
            val existingSubscription = getSubscriptionResponse.subscriptions.first()
            log.info { "Found existing distribution subscription ${user.email} with id ${existingSubscription.mySubscriptionId}" }
            if (user.distributionSubscriptionId == null) {
                user.distributionSubscriptionId = existingSubscription.mySubscriptionId
                userRepository.updateUserData(user.id!!, user)
            } else {
                require(user.distributionSubscriptionId == existingSubscription.mySubscriptionId) {
                    "User.distributionSubscriptionId: ${user.distributionSubscriptionId} does not match existing distribution subscription! ${existingSubscription.mySubscriptionId}"
                }
            }
        } else {
            require(user.distributionSubscriptionId == null) {
                "User.distributionSubscriptionId: ${user.distributionSubscriptionId} not found in Eveara!"
            }
            val response = addUserSubscription(user)
            log.info {
                "Created distribution subscription ${user.email} with id ${response.subscriptions[0].userSubscriptionId}: ${response.message}"
            }
            user.distributionSubscriptionId = response.subscriptions[0].userSubscriptionId
            userRepository.updateUserData(user.id!!, user)
        }
    }

    override suspend fun getSmartLinks(
        distributionUserId: String,
        distributionReleaseId: Long
    ): List<SmartLink> {
        val httpResponse = httpClient.get("$evearaApiBaseUrl/smartlinks/$distributionReleaseId") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(getEvearaApiToken())
            parameter("uuid", distributionUserId)
        }
        if (!httpResponse.status.isSuccess()) {
            throw ServerResponseException(httpResponse, "Error getting smart-links: ${httpResponse.bodyAsText()}")
        }
        val response: GetSmartLinksResponse = httpResponse.body()
        if (!response.success) {
            throw ServerResponseException(httpResponse, "Error getting smart-links: success==false")
        }
        return response.data.orEmpty()
    }

    private suspend fun createDistributionParticipants(
        user: User,
        collabs: List<Collaboration>
    ) {
        requireNotNull(user.id) { "User.id must not be null!" }

        // Create the newm participant if they don't exist yet
        var getParticipantsResponse = getParticipants(user)
        if (getParticipantsResponse.totalRecords > 0 &&
            getParticipantsResponse.participantData.any { it.name.equals(NEWM_PARTICIPANT_NAME, ignoreCase = true) }
        ) {
            val existingNewmParticipant = getParticipantsResponse.participantData.first {
                it.name.equals(
                    NEWM_PARTICIPANT_NAME,
                    ignoreCase = true
                )
            }
            log.info { "Found existing distribution participant $NEWM_PARTICIPANT_NAME with id ${existingNewmParticipant.participantId}" }
            if (user.distributionNewmParticipantId == null) {
                user.distributionNewmParticipantId = existingNewmParticipant.participantId
                userRepository.updateUserData(user.id, user)
            } else {
                require(user.distributionNewmParticipantId == existingNewmParticipant.participantId) {
                    "User.distributionNewmParticipantId: ${user.distributionNewmParticipantId} does not match existing distribution participant! ${existingNewmParticipant.participantId}"
                }
            }
        } else {
            require(user.distributionNewmParticipantId == null) {
                "User.distributionNewmParticipantId: ${user.distributionNewmParticipantId} not found in Eveara!"
            }
            val response =
                addParticipant(User(distributionUserId = user.distributionUserId!!, nickname = NEWM_PARTICIPANT_NAME))
            log.info { "Created $NEWM_PARTICIPANT_NAME participant with id ${response.participantId}: ${response.message}" }
            user.distributionNewmParticipantId = response.participantId
            userRepository.updateUserData(user.id, user)
            getParticipantsResponse = getParticipants(user)
        }

        // Create/Update the user participant (will pay to newm any royalties)
        if (getParticipantsResponse.totalRecords > 0) {
            val existingArtistParticipant =
                getParticipantsResponse.participantData.firstOrNull { it.participantId == user.distributionParticipantId }
            if (existingArtistParticipant != null) {
                log.info { "Found existing distribution participant ${user.email} with id ${existingArtistParticipant.participantId}" }
                if (existingArtistParticipant.name != user.stageOrFullName) {
                    log.info {
                        "Updating distribution participant ${user.email} with id ${existingArtistParticipant.participantId}, name ${existingArtistParticipant.name} -> ${user.stageOrFullName}"
                    }
                    val response = updateParticipant(user)
                    log.info { "Updated distribution participant ${user.email} with id ${user.distributionUserId}: ${response.message}" }
                    getParticipantsResponse = getParticipants(user)
                }
            } else {
                // See if we can find a participant where the name matches and then update the user record
                val existingParticipantNameMatch =
                    getParticipantsResponse.participantData.firstOrNull { it.name == user.stageOrFullName }
                if (existingParticipantNameMatch != null) {
                    user.distributionParticipantId = existingParticipantNameMatch.participantId
                    userRepository.updateUserData(user.id, user)
                    log.info {
                        "Found existing distribution participant ${user.email} with id ${existingParticipantNameMatch.participantId} by name match"
                    }
                } else {
                    log.info { "No existing distribution participant ${user.email} found by name match" }
                    val response = addParticipant(user)
                    log.info { "Created distribution participant ${user.email} with id ${response.participantId}: ${response.message}" }
                    user.distributionParticipantId = response.participantId
                    userRepository.updateUserData(user.id, user)
                    getParticipantsResponse = getParticipants(user)
                }
            }
        } else {
            require(
                user.distributionParticipantId == null
            ) { "User.distributionParticipantId: ${user.distributionParticipantId} not found in Eveara!" }
            val response = addParticipant(user)
            log.info { "Created distribution participant ${user.email} with id ${response.participantId}: ${response.message}" }
            user.distributionParticipantId = response.participantId
            userRepository.updateUserData(user.id, user)
            getParticipantsResponse = getParticipants(user)
        }

        // Create/Update the participantId for each collaborator (will pay to newm any royalties)
        val collabsToUpdate = mutableListOf<Collaboration>()
        collabs.forEach { collab ->
            val collabUser = userRepository.findByEmail(collab.email!!)
            val collabParticipant =
                getParticipantsResponse.participantData.firstOrNull { it.participantId == collab.distributionParticipantId }
            if (collabParticipant != null) {
                log.info { "Found existing distribution collab participant ${collabUser.email} with id ${collabParticipant.participantId}" }
                if (collabParticipant.name != collabUser.stageOrFullName) {
                    log.info {
                        "Updating distribution collab participant ${collabUser.email} with id ${collabParticipant.participantId}, name ${collabParticipant.name} -> ${collabUser.stageOrFullName}"
                    }
                    val response = updateParticipant(user, collabUser, collab)
                    log.info {
                        "Updated distribution collab participant ${collabUser.email} with id ${collabParticipant.participantId}: ${response.message}"
                    }
                    collabsToUpdate.add(collab.copy(distributionParticipantId = collabParticipant.participantId))
                    getParticipantsResponse = getParticipants(user)
                }
            } else {
                // See if we can find a participant where the name matches and then update the user record
                val existingParticipantNameMatch =
                    getParticipantsResponse.participantData.firstOrNull { it.name == collabUser.stageOrFullName }
                if (existingParticipantNameMatch != null) {
                    collabsToUpdate.add(collab.copy(distributionParticipantId = existingParticipantNameMatch.participantId))
                    log.info {
                        "Found existing distribution participant ${collabUser.email} with id ${existingParticipantNameMatch.participantId} by name match"
                    }
                } else {
                    log.info { "No existing distribution participant ${collabUser.email} found by name match" }
                    val response = addParticipant(user, collabUser, collab)
                    log.info {
                        "Created distribution collab participant ${collabUser.email} with id ${response.participantId}: ${response.message}"
                    }
                    collabsToUpdate.add(collab.copy(distributionParticipantId = response.participantId))
                    getParticipantsResponse = getParticipants(user)
                }
            }
        }
        if (collabsToUpdate.isNotEmpty()) {
            collabsToUpdate.forEach { collab ->
                collabRepository.update(
                    collab,
                    collab.id!!,
                    user.id,
                    skipStatusCheck = true
                )
            }
        }
    }

    private suspend fun createDistributionLabel(
        user: User,
        song: Song
    ) {
        requireNotNull(user.id) { "User.id must not be null!" }
        val desiredLabelName = song.phonographicCopyrightOwner ?: user.companyOrStageOrFullName
        val getUserLabelsResponse = getUserLabel(user)
        if (getUserLabelsResponse.totalRecords > 1) {
            val existingLabel =
                getUserLabelsResponse.userLabelData.first { !it.name.equals(NEWM_LABEL_NAME, ignoreCase = true) }
            log.info { "Found existing distribution label ${user.email} with id ${existingLabel.labelId}, name ${existingLabel.name}" }
            if (user.distributionLabelId == null) {
                user.distributionLabelId = existingLabel.labelId
                userRepository.updateUserData(user.id, user)
            } else {
                require(user.distributionLabelId == existingLabel.labelId) {
                    "User.distributionLabelId: ${user.distributionLabelId} does not match existing distribution label! ${existingLabel.labelId}"
                }
            }
            if (existingLabel.name != desiredLabelName) {
                log.info {
                    "Updating distribution label ${user.email} with id ${existingLabel.labelId}, name ${existingLabel.name} -> $desiredLabelName"
                }
                requireNotNull(user.distributionLabelId) { "User.distributionLabelId must not be null!" }
                requireNotNull(user.distributionUserId) { "User.distributionUserId must not be null!" }
                val response =
                    updateUserLabel(
                        user.distributionLabelId!!,
                        user.distributionUserId!!,
                        desiredLabelName,
                    )
                log.info { "Updated distribution label ${user.email} with id ${response.labelData?.labelId}: ${response.message}" }
            }
        } else {
            require(user.distributionLabelId == null) { "User.distributionLabelId: ${user.distributionLabelId} not found in Eveara!" }
            requireNotNull(user.distributionUserId) { "User.distributionUserId must not be null!" }
            val response =
                addUserLabel(
                    user.distributionUserId!!,
                    desiredLabelName,
                )
            log.info { "Created distribution label ${user.email} with id ${response.labelId}: ${response.message}" }
            user.distributionLabelId = response.labelId
            userRepository.updateUserData(user.id, user)
        }
    }

    /**
     * Upload and add metadata to the distribution track
     */
    private suspend fun createDistributionTrack(
        user: User,
        song: Song
    ): Song {
        var mutableSong = song
        if (mutableSong.distributionTrackId == null) {
            val s3Url = mutableSong.originalAudioUrl!!
            val (bucket, key) = s3Url.toBucketAndKey()
            val getObjectRequest = GetObjectRequest
                .builder()
                .bucket(bucket)
                .key(key)
                .build()

            var trackFile: File? = null
            try {
                trackFile = withContext(Dispatchers.IO) {
                    File.createTempFile("newm_track_", s3Url.getFileNameWithExtensionFromUrl())
                }
                val trackFilePath = trackFile.toPath()
                log.info { "Downloading track from $s3Url to ${trackFile.absolutePath} ..." }
                s3AsyncClient.getObject(getObjectRequest, trackFilePath).await()
                log.info { "Downloaded track ${mutableSong.title} to ${trackFile.absolutePath} having size ${trackFile.length()}" }

                val response = addTrack(user, trackFile)
                log.info { "Created distribution track ${mutableSong.title} with track_id ${response.trackId}: ${response.message}" }
                mutableSong = mutableSong.copy(distributionTrackId = response.trackId)
                songRepository.update(mutableSong.id!!, Song(distributionTrackId = response.trackId))
            } finally {
                if (trackFile?.delete() == true) {
                    log.info { "Deleted temp track file: ${trackFile.absolutePath}" }
                }
            }
        }

        // update track with collaborators and other metadata
        val updateTrackResponse = updateTrack(user, mutableSong.distributionTrackId!!, mutableSong)
        log.info {
            "Updated distribution track ${song.title} with track_id ${mutableSong.distributionTrackId}: ${updateTrackResponse.message}"
        }

        // update the isrc from eveara
        val getTrackResponse = getTracks(user, mutableSong.distributionTrackId)
        val stereoIsrc = getTrackResponse.trackData!!.first().stereoIsrc
        require(stereoIsrc.length == 12) { "Invalid stereo isrc: \"$stereoIsrc\"" }
        if (mutableSong.isrc != stereoIsrc) {
            // Save the isrc to the song
            val isrcCountry = stereoIsrc.substring(0, 2)
            val isrcRegistrant = stereoIsrc.substring(2, 5)
            val isrcYear = stereoIsrc.substring(5, 7)
            val isrcDesignation = stereoIsrc.substring(7, 12)
            val isrc = "$isrcCountry-$isrcRegistrant-$isrcYear-$isrcDesignation"
            log.info { "Updating song isrc from ${mutableSong.isrc} to $isrc" }
            mutableSong = mutableSong.copy(isrc = isrc)
            songRepository.update(mutableSong.id!!, Song(isrc = isrc))
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
        return mutableSong
    }

    private suspend fun createDistributionAlbum(
        user: User,
        release: Release,
        songs: List<Song>
    ): Release {
        var mutableRelease = release
        val getAlbumResponse = getAlbums(user)
        val existingAlbum =
            if (getAlbumResponse.totalRecords > 0) {
                getAlbumResponse.albumData.firstOrNull {
                    it.releaseId == mutableRelease.distributionReleaseId ||
                        it.name == mutableRelease.title
                }
            } else {
                null
            }

        if (existingAlbum == null) {
            val response = addAlbum(user, release, songs)
            log.info { "Created distribution album ${release.title} with id ${response.releaseId}: ${response.message}" }
            val albumData = getAlbums(user).albumData.first { it.releaseId == response.releaseId }
            val barcodeType = albumData.productCodeType.toSongBarcodeType()
            val barcode = albumData.eanUpc
            mutableRelease =
                mutableRelease.copy(
                    distributionReleaseId = response.releaseId,
                    barcodeNumber = barcode,
                    barcodeType = barcodeType
                )
            songRepository.update(
                mutableRelease.id!!,
                Release(
                    distributionReleaseId = response.releaseId,
                    barcodeNumber = barcode,
                    barcodeType = barcodeType,
                    preSavePage =
                        response.releaseId
                            .toString()
                            .toByteArray()
                            .toB64String()
                )
            )
        } else {
            log.info {
                "Found existing distribution album ${mutableRelease.title} with id ${existingAlbum.releaseId}, title ${existingAlbum.name}"
            }
            if (mutableRelease.distributionReleaseId == null) {
                mutableRelease = mutableRelease.copy(distributionReleaseId = existingAlbum.releaseId)
                songRepository.update(mutableRelease.id!!, Release(distributionReleaseId = existingAlbum.releaseId))
            } else {
                require(mutableRelease.distributionReleaseId == existingAlbum.releaseId) {
                    "Song.distributionReleaseId: ${mutableRelease.distributionReleaseId} does not match existing distribution album! ${existingAlbum.releaseId}"
                }
            }
            val response = updateAlbum(user, mutableRelease, songs)
            log.info { "Updated distribution album ${mutableRelease.title} with id ${existingAlbum.releaseId}: ${response.message}" }
        }
        return mutableRelease
    }

    private suspend fun distributeAlbumRelease(
        user: User,
        release: Release
    ): Release {
        requireNotNull(user.id) { "User.id must not be null!" }
        var mutableRelease = release

        // Only allow extending release date if we haven't attempted distribution before.
        if (mutableRelease.hasSubmittedForDistribution != true) {
            // Verify we have enough days to distribute the release
            val earliestReleaseDate = getEarliestReleaseDate(user.id)
            if (earliestReleaseDate.isAfter(mutableRelease.releaseDate!!)) {
                mutableRelease = mutableRelease.copy(releaseDate = earliestReleaseDate)
                songRepository.update(mutableRelease.id!!, Release(releaseDate = earliestReleaseDate))
            }
        }

        // Distribute the release to outlets
        val distributeReleaseResponse = distributeReleaseToOutlets(user, mutableRelease)
        require(
            distributeReleaseResponse.releaseData?.errorFields.isNullOrEmpty()
        ) { "Error distributing release: $distributeReleaseResponse" }
        log.info {
            "Distributed release ${mutableRelease.title} with id ${mutableRelease.distributionReleaseId} to outlets: ${distributeReleaseResponse.message}"
        }

        // Distribute the release to future outlets
        val distributeFutureReleaseResponse =
            distributeReleaseToFutureOutlets(user, mutableRelease.distributionReleaseId!!)
        log.info {
            "Distributed release ${mutableRelease.title} with id ${mutableRelease.distributionReleaseId} to future outlets: ${distributeFutureReleaseResponse.message}"
        }
        return mutableRelease
    }

    private fun collectSongArtistIdsList(
        user: User,
        collabs: List<Collaboration>
    ): List<Long> =
        listOf(user.distributionArtistId!!) +
            collabs
                .filter {
                    it.roles?.containsIgnoreCase("Artist") == true &&
                        it.featured == false &&
                        it.email != user.email
                }.map { it.distributionArtistId!! }
                .distinct()

    private fun collectFeaturedArtistIdsList(
        user: User,
        collabs: List<Collaboration>,
    ): List<Long>? =
        collabs
            .filter { it.featured == true && it.email != user.email }
            .map { it.distributionArtistId!! }
            .distinct()
            .takeIf { it.isNotEmpty() }

    private suspend fun collectSongParticipantsList(
        user: User,
        collabs: List<Collaboration>
    ): List<Participant> =
        (
            listOf(
                Participant(
                    id = user.distributionNewmParticipantId!!,
                    roleId =
                        listOf(
                            getRoles()
                                .roles
                                .first {
                                    it.name.equals(
                                        "Publisher",
                                        ignoreCase = true
                                    )
                                }.roleId
                        ),
                    // NEWM takes 100% so we can then pay out based on stream token holdings
                    payoutSharePercentage = 100,
                )
            ) +
                collabs.map { collab ->
                    Participant(
                        id = collab.distributionParticipantId!!,
                        roleId =
                            listOf(
                                getRoles()
                                    .roles
                                    .first {
                                        collab.roles?.containsIgnoreCase(it.name) == true
                                    }.roleId
                            ),
                        payoutSharePercentage = 0,
                    )
                }
        ).distinctBy { "${it.id}|${it.roleId}" }

    private fun Long.toReleaseDate() = LocalDate.now().plusDays(this + 2)

    private suspend fun getUserCountryCode(user: User): String {
        val countryCode =
            getCountries()
                .countries
                .firstOrNull {
                    it.countryCode.equals(user.location, ignoreCase = true) ||
                        it.countryName.equals(user.location, ignoreCase = true)
                }?.countryCode

        requireNotNull(countryCode) { "Country code for profile location ${user.location} not found!" }
        return countryCode
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
        private const val NEWM_LABEL_NAME = "NEWM"
        private const val NEWM_PARTICIPANT_NAME = "NEWM"
    }
}
