package io.newm.chain.database.table

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object LedgerTable : LongIdTable(name = "ledger") {
    // Address that holds utxos on the ledger
    val address: Column<String> = text("address")

    // Stake address portion of the address (optional)
    val stakeAddress: Column<String?> = text("stake_address").nullable()

    // hex byte that determines the type of address this is
    val addressType: Column<String> = varchar("address_type", 2)
}
