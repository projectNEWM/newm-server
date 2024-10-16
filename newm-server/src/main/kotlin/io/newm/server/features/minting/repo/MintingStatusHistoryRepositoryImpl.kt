package io.newm.server.features.minting.repo

import io.github.oshai.kotlinlogging.KotlinLogging
import io.newm.server.features.minting.database.MintingStatusTransactionEntity
import io.newm.server.features.minting.model.MintingStatusTransactionModel
import io.newm.server.typealiases.SongId
import org.jetbrains.exposed.sql.transactions.transaction

internal class MintingStatusHistoryRepositoryImpl : MintingStatusHistoryRepository {
    private val logger = KotlinLogging.logger {}

    override suspend fun add(mintingStatusTransactionEntity: MintingStatusTransactionEntity) =
        transaction {
            MintingStatusTransactionEntity
                .new {
                    mintingStatus = mintingStatusTransactionEntity.mintingStatus
                    createdAt = mintingStatusTransactionEntity.createdAt
                    logMessage = mintingStatusTransactionEntity.logMessage
                    songId = mintingStatusTransactionEntity.songId
                }.id.value
        }

    override fun getMintingStatusHistoryEntity(songId: SongId): List<MintingStatusTransactionModel> {
        logger.debug { "get minting history for : songId = $songId" }
        return transaction {
            MintingStatusTransactionEntity
                .all()
                .map { it.toModel() }
        }
    }
}
