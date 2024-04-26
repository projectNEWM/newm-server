package io.newm.server.features.song.repo

import io.ktor.utils.io.*
import io.newm.chain.grpc.Utxo
import io.newm.server.features.song.database.SongEntity
import io.newm.server.features.song.model.*
import java.util.*

interface SongRepository {
    suspend fun add(
        song: Song,
        ownerId: UUID
    ): UUID

    suspend fun update(
        songId: UUID,
        song: Song,
        requesterId: UUID? = null
    )

    suspend fun update(
        releaseId: UUID,
        release: Release,
        requesterId: UUID? = null
    )

    suspend fun delete(
        songId: UUID,
        requesterId: UUID
    )

    suspend fun get(songId: UUID): Song

    suspend fun getRelease(releaseId: UUID): Release

    suspend fun getAll(
        filters: SongFilters,
        offset: Int,
        limit: Int
    ): List<Song>

    suspend fun getAllCount(filters: SongFilters): Long

    suspend fun getAllByReleaseId(id: UUID): List<Song>

    suspend fun getGenres(
        filters: SongFilters,
        offset: Int,
        limit: Int
    ): List<String>

    suspend fun getGenreCount(filters: SongFilters): Long

    suspend fun uploadAudio(
        songId: UUID,
        requesterId: UUID,
        data: ByteReadChannel
    ): AudioUploadReport

    suspend fun generateAudioStreamData(songId: UUID): AudioStreamData

    suspend fun processStreamTokenAgreement(
        songId: UUID,
        requesterId: UUID,
        accepted: Boolean
    )

    suspend fun processAudioEncoding(songId: UUID)

    suspend fun getMintingPaymentAmount(
        songId: UUID,
        requesterId: UUID
    ): MintPaymentResponse

    suspend fun getMintingPaymentEstimate(collaborators: Int): MintPaymentResponse

    suspend fun generateMintingPaymentTransaction(
        songId: UUID,
        requesterId: UUID,
        sourceUtxos: List<Utxo>,
        changeAddress: String
    ): String

    suspend fun refundMintingPayment(
        songId: UUID,
        walletAddress: String
    ): RefundPaymentResponse

    suspend fun processCollaborations(songId: UUID)

    suspend fun updateSongMintingStatus(
        songId: UUID,
        mintingStatus: MintingStatus,
        errorMessage: String = ""
    )

    suspend fun distribute(songId: UUID)

    suspend fun redistribute(songId: UUID)

    fun set(
        songId: UUID,
        editor: (SongEntity) -> Unit
    )

    fun saveOrUpdateReceipt(
        songId: UUID,
        mintPaymentResponse: MintPaymentResponse
    )
}
