package io.newm.server.features.minting.repo

import io.newm.server.features.minting.database.MintingStatusTransactionEntity
import io.newm.server.features.minting.model.MintingStatusTransactionModel
import io.newm.server.typealiases.SongId
import java.util.UUID

interface MintingStatusHistoryRepository {
    suspend fun add(mintingStatusTransactionEntity: MintingStatusTransactionEntity): UUID

    fun getMintingStatusHistoryEntity(songId: SongId): List<MintingStatusTransactionModel>
}
