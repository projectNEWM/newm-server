package io.newm.server.features.distribution

import io.newm.server.features.distribution.model.AddArtistRequest
import io.newm.server.features.distribution.model.AddArtistResponse
import io.newm.server.features.distribution.model.AddParticipantPaypalResponse
import io.newm.server.features.distribution.model.AddParticipantResponse
import io.newm.server.features.distribution.model.AddTrackResponse
import io.newm.server.features.distribution.model.AddUserLabelResponse
import io.newm.server.features.distribution.model.AddUserResponse
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
import io.newm.server.features.distribution.model.UpdateTrackResponse
import io.newm.server.features.distribution.model.UpdateUserLabelResponse
import io.newm.server.features.song.database.SongEntity
import io.newm.server.features.song.model.Song
import io.newm.server.features.user.model.User
import java.io.File
import java.util.UUID

/**
 * Higher level api for working with a music distribution service
 */
interface DistributionRepository {
    suspend fun getGenres(): GetGenresResponse

    suspend fun getRoles(): GetRolesResponse

    suspend fun getLanguages(): GetLanguagesResponse

    suspend fun getCountries(): GetCountriesResponse

    suspend fun getOutlets(): GetOutletsResponse

    suspend fun addUser(user: User): AddUserResponse

    suspend fun getUser(userId: UUID): GetUserResponse

    suspend fun addUserSubscription(user: User): AddUserSubscriptionResponse

    suspend fun getUserSubscription(user: User): GetUserSubscriptionResponse

    suspend fun addUserLabel(user: User): AddUserLabelResponse

    suspend fun updateUserLabel(user: User): UpdateUserLabelResponse

    suspend fun deleteUserLabel(user: User): DeleteUserLabelResponse

    suspend fun addArtist(addArtistRequest: AddArtistRequest): AddArtistResponse

    suspend fun getArtist(artistId: String): GetArtistResponse

    suspend fun addParticipant(user: User): AddParticipantResponse

    suspend fun updateParticipant(user: User): AddParticipantResponse

    /**
     * We should only need to call this function for the main newm user. Regular participants will not need paypal
     * accounts.
     */
    suspend fun addParticipantPaypal(user: User, paypalEmail: String): AddParticipantPaypalResponse

    suspend fun addTrack(evearaUserId: UUID, trackFile: File): AddTrackResponse

    suspend fun updateTrack(trackId: Long, user: User, song: Song): UpdateTrackResponse

    suspend fun distributeSong(song: SongEntity)
}
