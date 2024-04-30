package io.newm.server.features.earnings.database

import io.newm.server.features.song.database.SongTable
import io.newm.server.typealiases.SongId
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.UUID

object EarningsTable : UUIDTable(name = "earnings") {
    val songId: Column<EntityID<SongId>?> =
        reference("song_id", SongTable, onDelete = ReferenceOption.RESTRICT).nullable()
    val stakeAddress: Column<String> = text("stake_address")
    val amount: Column<Long> = long("amount")
    val memo: Column<String> = text("memo")
    val startDate: Column<LocalDateTime?> = datetime("start_date").nullable()
    val endDate: Column<LocalDateTime?> = datetime("end_date").nullable()
    val claimed: Column<Boolean> = bool("claimed").default(false)
    val claimedAt: Column<LocalDateTime?> = datetime("claimed_at").nullable()
    val claimedOrderId: Column<EntityID<UUID>?> =
        reference(
            "claimed_order_id",
            ClaimOrdersTable,
            onUpdate = ReferenceOption.RESTRICT,
            onDelete = ReferenceOption.RESTRICT
        ).nullable()
    val createdAt: Column<LocalDateTime> = datetime("created_at").defaultExpression(CurrentDateTime)
}
