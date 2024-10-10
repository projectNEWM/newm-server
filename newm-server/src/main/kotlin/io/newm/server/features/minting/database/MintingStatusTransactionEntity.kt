package io.newm.server.features.minting.database

import io.newm.server.features.minting.model.MintingStatusTransactionModel
import io.newm.server.features.song.model.MintingStatus
import io.newm.server.typealiases.SongId
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.time.LocalDateTime
import java.util.UUID

class MintingStatusTransactionEntity(
    id: EntityID<UUID>
) : UUIDEntity(id) {
    var mintingStatus: MintingStatus by MintingStatusHistoryTable.mintingStatus
    var timestamp: LocalDateTime by MintingStatusHistoryTable.timestamp
    var logMessage: String? by MintingStatusHistoryTable.logMessage
    lateinit var songId: SongId

    fun toModel(): MintingStatusTransactionModel =
        MintingStatusTransactionModel(
            id = id.value,
            mintingStatus = mintingStatus,
            timestamp = timestamp,
            logMessage = logMessage,
            songId = songId
        )

    companion object : UUIDEntityClass<MintingStatusTransactionEntity>(MintingStatusHistoryTable) {
    }
}
