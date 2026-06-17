package io.newm.chain.database.table

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object LedgerAssetsTable : LongIdTable(name = "ledger_assets") {
    // policy id for this asset
    val policy: Column<String> = text("policy")

    // name for this asset
    val name: Column<String> = text("name")

    // the total supply of this asset
    val supply: Column<String> = text("supply")
}
