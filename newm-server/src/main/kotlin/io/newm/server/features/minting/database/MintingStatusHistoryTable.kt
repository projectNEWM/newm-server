package io.newm.server.features.minting.database

import io.newm.server.features.song.database.SongTable
import io.newm.server.features.song.database.SongTable.default
import io.newm.server.features.song.model.MintingStatus
import io.newm.server.features.walletconnection.database.WalletConnectionTable.nullable
import io.newm.server.typealiases.SongId
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object MintingStatusHistoryTable : UUIDTable(name = "minting_status_history") {
    val timestamp: Column<LocalDateTime> = datetime("time_stamp").defaultExpression(CurrentDateTime)
    val mintingStatus: Column<MintingStatus> = enumeration("minting_status", MintingStatus::class).default(MintingStatus.Undistributed)
    val logMessage: Column<String?> = text("log_message").nullable()
    val songId: Column<EntityID<SongId>?> = reference("song_id", SongTable, onDelete = ReferenceOption.CASCADE).nullable()
}
