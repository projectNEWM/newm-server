package io.newm.server.features.minting.repo

import io.newm.server.features.minting.database.MintingStatusTransactionEntity
import io.newm.server.features.minting.model.MintInfo
import io.newm.server.features.minting.model.MintingStatusTransactionModel
import io.newm.server.features.song.model.Song
import io.newm.server.typealiases.SongId
import java.util.*

interface MintingRepository {
    /**
     * Mints the song onto the blockchain and returns the successful transactionId
     */
    suspend fun mint(song: Song): MintInfo

    fun getTokenAgreementFileIndex(policyId: String): Int

    fun getAudioClipFileIndex(policyId: String): Int

    suspend fun add(mintingStatusTransactionEntity: MintingStatusTransactionEntity): UUID

    fun getMintingStatusHistoryEntity(songId: SongId): List<MintingStatusTransactionModel>
}
