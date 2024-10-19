package io.newm.server.features.minting.database

import io.newm.server.features.minting.model.MintingStatusTransactionModel
import io.newm.server.typealiases.SongId
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.time.LocalDateTime
import java.util.UUID

class MintingStatusTransactionEntity(
    id: EntityID<UUID>
) : UUIDEntity(id) {
    var mintingStatus: String by MintingStatusHistoryTable.mintingStatus
    var createdAt: LocalDateTime by MintingStatusHistoryTable.createdAt
    var logMessage: String? by MintingStatusHistoryTable.logMessage
    var songId: EntityID<SongId> by MintingStatusHistoryTable.songId

    fun toModel(): MintingStatusTransactionModel =
        MintingStatusTransactionModel(
            id = id.value,
            mintingStatus = mintingStatus,
            createdAt = createdAt,
            logMessage = logMessage,
            songId = songId.value
        )

    companion object : UUIDEntityClass<MintingStatusTransactionEntity>(MintingStatusHistoryTable) {
    }
}
