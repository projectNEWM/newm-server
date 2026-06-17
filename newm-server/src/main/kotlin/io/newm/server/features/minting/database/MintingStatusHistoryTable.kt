package io.newm.server.features.minting.database

import io.newm.server.features.song.database.SongTable
import io.newm.server.features.walletconnection.database.WalletConnectionTable.nullable
import io.newm.server.typealiases.SongId
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.javatime.CurrentDateTime
import org.jetbrains.exposed.v1.javatime.datetime
import java.time.LocalDateTime

object MintingStatusHistoryTable : UUIDTable(name = "minting_status_history") {
    val createdAt: Column<LocalDateTime> = datetime("created_at").defaultExpression(CurrentDateTime)
    val mintingStatus: Column<String> = text("minting_status")
    val logMessage: Column<String?> = text("log_message").nullable()
    val songId: Column<EntityID<SongId>> = reference("song_id", SongTable, onDelete = ReferenceOption.NO_ACTION)
}
