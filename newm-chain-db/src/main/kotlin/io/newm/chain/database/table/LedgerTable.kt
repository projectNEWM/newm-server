package io.newm.chain.database.table

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column

object LedgerTable : LongIdTable(name = "ledger") {
    // Address that holds utxos on the ledger
    val address: Column<String> = text("address")

    // Stake address portion of the address (optional)
    val stakeAddress: Column<String?> = text("stake_address").nullable()

    // hex byte that determines the type of address this is
    val addressType: Column<String> = varchar("address_type", 2)
}
