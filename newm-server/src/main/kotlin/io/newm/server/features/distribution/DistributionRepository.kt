package io.newm.server.features.distribution

import io.newm.server.features.collaboration.model.Collaboration
import io.newm.server.features.distribution.model.AddAlbumResponse
import io.newm.server.features.distribution.model.AddArtistRequest
import io.newm.server.features.distribution.model.AddArtistResponse
import io.newm.server.features.distribution.model.AddParticipantResponse
import io.newm.server.features.distribution.model.AddTrackResponse
import io.newm.server.features.distribution.model.AddUserLabelResponse
import io.newm.server.features.distribution.model.AddUserResponse
import io.newm.server.features.distribution.model.AddUserSubscriptionResponse
import io.newm.server.features.distribution.model.DeleteUserLabelResponse
import io.newm.server.features.distribution.model.DistributeReleaseResponse
import io.newm.server.features.distribution.model.DistributionOutletReleaseStatusResponse
import io.newm.server.features.distribution.model.EvearaSimpleResponse
import io.newm.server.features.distribution.model.GetAlbumResponse
import io.newm.server.features.distribution.model.GetArtistResponse
import io.newm.server.features.distribution.model.GetCountriesResponse
import io.newm.server.features.distribution.model.GetGenresResponse
import io.newm.server.features.distribution.model.GetLanguagesResponse
import io.newm.server.features.distribution.model.GetOutletProfileNamesResponse
import io.newm.server.features.distribution.model.GetOutletsResponse
import io.newm.server.features.distribution.model.GetParticipantsResponse
import io.newm.server.features.distribution.model.GetRolesResponse
import io.newm.server.features.distribution.model.GetTracksResponse
import io.newm.server.features.distribution.model.GetUserLabelResponse
import io.newm.server.features.distribution.model.GetUserResponse
import io.newm.server.features.distribution.model.GetUserSubscriptionResponse
import io.newm.server.features.distribution.model.UpdateArtistRequest
import io.newm.server.features.distribution.model.UpdateArtistResponse
import io.newm.server.features.distribution.model.UpdateUserLabelResponse
import io.newm.server.features.distribution.model.ValidateAlbumResponse
import io.newm.server.features.song.model.Song
import io.newm.server.features.user.model.User
import java.io.File
import java.time.LocalDate
import java.util.UUID

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
        trackId: Long,
        song: Song
    ): AddAlbumResponse

    suspend fun getAlbums(user: User): GetAlbumResponse

    suspend fun updateAlbum(
        user: User,
        song: Song
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
        releaseStartDate: LocalDate,
        releaseId: Long
    ): DistributeReleaseResponse

    suspend fun distributeReleaseToFutureOutlets(
        user: User,
        releaseId: Long
    ): DistributeReleaseResponse

    suspend fun distributionOutletReleaseStatus(
        user: User,
        releaseId: Long
    ): DistributionOutletReleaseStatusResponse

    suspend fun distributeSong(song: Song)

    suspend fun getEarliestReleaseDate(userId: UUID): LocalDate
}
