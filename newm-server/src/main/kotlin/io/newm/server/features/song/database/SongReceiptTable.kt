package io.newm.server.features.song.database

import io.newm.server.typealiases.SongId
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object SongReceiptTable : UUIDTable(name = "song_receipts") {
    val createdAt: Column<LocalDateTime> = datetime("created_at").defaultExpression(CurrentDateTime)
    val songId: Column<EntityID<SongId>> = reference("song_id", SongTable, onDelete = ReferenceOption.NO_ACTION)
    val adaPrice: Column<Long> = long("ada_price")
    val usdPrice: Column<Long> = long("usd_price")
    val adaDspPrice: Column<Long> = long("ada_dsp_price")
    val usdDspPrice: Column<Long> = long("usd_dsp_price")
    val adaMintPrice: Column<Long> = long("ada_mint_price")
    val usdMintPrice: Column<Long> = long("usd_mint_price")
    val adaCollabPrice: Column<Long> = long("ada_collab_price")
    val usdCollabPrice: Column<Long> = long("usd_collab_price")
    val usdAdaExchangeRate: Column<Long> = long("usd_ada_exchange_rate")
    val newmPrice: Column<Long> = long("newm_price")
    val newmDspPrice: Column<Long> = long("newm_dsp_price")
    val newmMintPrice: Column<Long> = long("newm_mint_price")
    val newmCollabPrice: Column<Long> = long("newm_collab_price")
    val usdNewmExchangeRate: Column<Long> = long("usd_newm_exchange_rate")
}
