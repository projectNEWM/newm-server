package io.newm.chain.database.table

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column

object MonitoredAddressChainTable : LongIdTable(name = "monitored_address_chain") {
    // the address
    val address: Column<String> = text("address")

    // the height
    val height: Column<Long> = long("height")

    // the slot
    val slot: Column<Long> = long("slot")

    // the hash
    val hash: Column<String> = text("hash")
}
