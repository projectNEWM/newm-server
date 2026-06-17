package io.newm.chain.database.table

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object LedgerUtxoAssetsTable : LongIdTable(name = "ledger_utxo_assets") {
    val ledgerUtxoId: Column<Long> = long("ledger_utxo_id").references(LedgerUtxosTable.id)
    val ledgerAssetId: Column<Long> = long("ledger_asset_id").references(LedgerAssetsTable.id)
    val amount: Column<String> = text("amount")
}
