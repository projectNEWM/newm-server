package io.newm.chain.database.table

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column

object LedgerAssetsTable : LongIdTable(name = "ledger_assets") {
    // policy id for this asset
    val policy: Column<String> = text("policy")

    // name for this asset
    val name: Column<String> = text("name")

    // the asset image ipfs link
    val image: Column<String?> = text("image").nullable()

    // the asset image description
    val description: Column<String?> = text("description").nullable()
}
