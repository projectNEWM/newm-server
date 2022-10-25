package io.newm.chain.database.table

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column

object RawTransactionsTable : LongIdTable(name = "raw_transactions") {
    // the block number
    val blockNumber: Column<Long> = long("block_number")

    // the slot number
    val slotNumber: Column<Long> = long("slot_number")

    // the block size
    val blockSize: Column<Int> = integer("block_size")

    // the block body hash in hex
    val blockBodyHash: Column<String> = text("block_body_hash")

    // the major number
    val protocolVersionMajor: Column<Int> = integer("protocol_version_major")

    // the minor number
    val protocolVersionMinor: Column<Int> = integer("protocol_version_minor")

    // transaction id
    val txId: Column<String> = text("tx_id")

    // the raw transaction cbor bytes
    val tx: Column<ByteArray> = binary("tx")
}
