package io.newm.chain.database.table

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column

object LedgerUtxoAssetsTable : LongIdTable(name = "ledger_utxo_assets") {
    val ledgerUtxoId: Column<Long> = long("ledger_utxo_id").references(LedgerUtxosTable.id)
    val ledgerAssetId: Column<Long> = long("ledger_asset_id").references(LedgerAssetsTable.id)
    val amount: Column<String> = text("amount")
}
