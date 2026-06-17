package io.newm.chain.database.table

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

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
