package io.newm.chain.database.table

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object AddressTxLogTable : LongIdTable(name = "address_tx_log") {
    val address: Column<String> = text("address")
    val txId: Column<String> = text("tx_id")
    val monitorAddressResponseBytes: Column<ByteArray> = binary("monitor_address_response")
    val blockNumber: Column<Long> = long("block_number")
}
