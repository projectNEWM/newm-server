package io.newm.server.features.song.database

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.UUID

object SongReceiptTable : UUIDTable(name = "song_receipts") {
    val createdAt: Column<LocalDateTime> = datetime("created_at").defaultExpression(CurrentDateTime)
    val songId: Column<EntityID<UUID>> = reference("song_id", SongTable, onDelete = ReferenceOption.CASCADE)
    val adaPrice: Column<Long> = long("ada_price")
    val usdPrice: Column<Long> = long("usd_price")
    val adaDspPrice: Column<Long> = long("ada_dsp_price")
    val usdDspPrice: Column<Long> = long("usd_dsp_price")
    val adaMintPrice: Column<Long> = long("ada_mint_price")
    val usdMintPrice: Column<Long> = long("usd_mint_price")
    val adaCollabPrice: Column<Long> = long("ada_collab_price")
    val usdCollabPrice: Column<Long> = long("usd_collab_price")
    val usdAdaExchangeRate: Column<Long> = long("usd_ada_exchange_rate")
}
