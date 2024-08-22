package io.newm.server.features.distribution

import io.newm.server.features.collaboration.model.Collaboration
import io.newm.server.features.distribution.model.*
import io.newm.server.features.song.model.Release
import io.newm.server.features.song.model.Song
import io.newm.server.features.user.model.User
import io.newm.server.typealiases.UserId
import java.io.File
import java.time.LocalDate
import java.util.*

/**
 * Higher level api for working with a music distribution service
 */
interface DistributionRepository {
    suspend fun getGenres(): GetGenresResponse

    suspend fun getRoles(): GetRolesResponse

    suspend fun getLanguages(): GetLanguagesResponse

    suspend fun getCountries(): GetCountriesResponse

    suspend fun getOutlets(user: User): GetOutletsResponse

    suspend fun addUser(user: User): AddUserResponse

    suspend fun getUser(user: User): GetUserResponse

    suspend fun updateUser(user: User): EvearaSimpleResponse

    suspend fun addUserSubscription(user: User): AddUserSubscriptionResponse

    suspend fun getUserSubscription(user: User): GetUserSubscriptionResponse

    suspend fun addUserLabel(
        distributionUserId: String,
        label: String
    ): AddUserLabelResponse

    suspend fun getUserLabel(user: User): GetUserLabelResponse

    suspend fun updateUserLabel(
        distributionLabelId: Long,
        distributionUserId: String,
        label: String
    ): UpdateUserLabelResponse

    suspend fun deleteUserLabel(user: User): DeleteUserLabelResponse

    suspend fun addArtist(addArtistRequest: AddArtistRequest): AddArtistResponse

    suspend fun getArtist(user: User): GetArtistResponse

    suspend fun getArtists(user: User): GetArtistResponse

    suspend fun updateArtist(
        artistId: Long,
        updateArtistRequest: UpdateArtistRequest
    ): UpdateArtistResponse

    suspend fun getArtistOutletProfileNames(user: User): GetOutletProfileNamesResponse

    suspend fun addParticipant(
        user: User,
        collabUser: User? = null,
        collab: Collaboration? = null,
    ): AddParticipantResponse

    suspend fun updateParticipant(
        user: User,
        collabUser: User? = null,
        collab: Collaboration? = null,
    ): EvearaSimpleResponse

    suspend fun getParticipants(user: User): GetParticipantsResponse

    suspend fun addTrack(
        user: User,
        trackFile: File
    ): AddTrackResponse

    suspend fun updateTrack(
        user: User,
        trackId: Long,
        song: Song
    ): EvearaSimpleResponse

    suspend fun getTracks(
        user: User,
        trackId: Long? = null
    ): GetTracksResponse

    suspend fun deleteTrack(
        user: User,
        trackId: Long
    ): EvearaSimpleResponse

    suspend fun isTrackStatusCompleted(
        user: User,
        trackId: Long
    ): Boolean

    suspend fun addAlbum(
        user: User,
        release: Release,
        songs: List<Song>
    ): AddAlbumResponse

    suspend fun getAlbums(user: User): GetAlbumResponse

    suspend fun updateAlbum(
        user: User,
        release: Release,
        songs: List<Song>
    ): EvearaSimpleResponse

    suspend fun validateAlbum(
        user: User,
        releaseId: Long
    ): ValidateAlbumResponse

    suspend fun deleteAlbum(
        user: User,
        releaseId: Long
    ): EvearaSimpleResponse

    suspend fun simulateDistributeRelease(
        user: User,
        releaseId: Long
    ): EvearaSimpleResponse

    suspend fun distributeReleaseToOutlets(
        user: User,
        release: Release,
        allowRetry: Boolean = true,
    ): DistributeReleaseResponse

    suspend fun distributeReleaseToFutureOutlets(
        user: User,
        releaseId: Long
    ): DistributeReleaseResponse

    suspend fun distributionOutletReleaseStatus(
        user: User,
        releaseId: Long
    ): DistributionOutletReleaseStatusResponse

    suspend fun distributeRelease(release: Release)

    suspend fun redistributeRelease(release: Release)

    suspend fun getEarliestReleaseDate(userId: UserId): LocalDate

    suspend fun getPayoutBalance(userId: UserId): GetPayoutBalanceResponse

    suspend fun getPayoutHistory(userId: UserId): GetPayoutHistoryResponse

    suspend fun initiatePayout(userId: UserId): InitiatePayoutResponse

    suspend fun createDistributionUserIfNeeded(user: User)

    suspend fun createDistributionSubscription(user: User)
}
