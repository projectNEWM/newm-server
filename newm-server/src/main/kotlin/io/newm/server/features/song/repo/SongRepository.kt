package io.newm.server.features.song.repo

import io.ktor.utils.io.ByteReadChannel
import io.newm.chain.grpc.Utxo
import io.newm.server.features.song.database.SongEntity
import io.newm.server.features.song.model.AudioStreamData
import io.newm.server.features.song.model.AudioUploadReport
import io.newm.server.features.song.model.MintPaymentResponse
import io.newm.server.features.song.model.MintingStatus
import io.newm.server.features.song.model.PaymentType
import io.newm.server.features.song.model.RefundPaymentResponse
import io.newm.server.features.song.model.Release
import io.newm.server.features.song.model.Song
import io.newm.server.features.song.model.SongFilters
import io.newm.server.features.song.model.SongSmartLink
import io.newm.server.typealiases.ReleaseId
import io.newm.server.typealiases.SongId
import io.newm.server.typealiases.UserId

interface SongRepository {
    suspend fun add(
        song: Song,
        ownerId: UserId
    ): SongId

    suspend fun update(
        songId: SongId,
        song: Song,
        requesterId: UserId? = null
    )

    suspend fun update(
        releaseId: ReleaseId,
        release: Release,
        requesterId: UserId? = null
    )

    suspend fun delete(
        songId: SongId,
        requesterId: UserId
    )

    suspend fun get(songId: SongId): Song

    suspend fun getRelease(releaseId: ReleaseId): Release

    suspend fun getAll(
        filters: SongFilters,
        offset: Int,
        limit: Int
    ): List<Song>

    suspend fun getAllCount(filters: SongFilters): Long

    suspend fun getAllByReleaseId(id: ReleaseId): List<Song>

    suspend fun getGenres(
        filters: SongFilters,
        offset: Int,
        limit: Int
    ): List<String>

    suspend fun getGenreCount(filters: SongFilters): Long

    suspend fun uploadAudio(
        songId: SongId,
        requesterId: UserId,
        data: ByteReadChannel
    ): AudioUploadReport

    suspend fun generateAudioStreamData(songId: SongId): AudioStreamData

    suspend fun processStreamTokenAgreement(
        songId: SongId,
        requesterId: UserId,
        accepted: Boolean
    )

    suspend fun processAudioEncoding(songId: SongId)

    suspend fun getMintingPaymentAmount(
        songId: SongId,
        paymentType: PaymentType,
    ): MintPaymentResponse

    suspend fun getMintingPaymentEstimate(collaborators: Int): MintPaymentResponse

    suspend fun generateMintingPaymentTransaction(
        songId: SongId,
        requesterId: UserId,
        sourceUtxos: List<Utxo>,
        changeAddress: String
    ): String

    suspend fun refundMintingPayment(
        songId: SongId,
        walletAddress: String
    ): RefundPaymentResponse

    suspend fun processCollaborations(songId: SongId)

    suspend fun updateSongMintingStatus(
        songId: SongId,
        mintingStatus: MintingStatus,
        errorMessage: String = ""
    )

    suspend fun distribute(songId: SongId)

    suspend fun redistribute(songId: SongId)

    fun set(
        songId: SongId,
        editor: (SongEntity) -> Unit
    )

    fun saveOrUpdateReceipt(
        songId: SongId,
        mintPaymentResponse: MintPaymentResponse
    )

    suspend fun getSmartLinks(songId: SongId): List<SongSmartLink>
}
