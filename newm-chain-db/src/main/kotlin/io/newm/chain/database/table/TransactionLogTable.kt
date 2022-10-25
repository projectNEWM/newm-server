package io.newm.chain.database.table

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column

object TransactionLogTable : LongIdTable(name = "transaction_log") {
    val transactionId: Column<String> = text("transaction_id")
    val cbor: Column<String> = text("cbor")
    val timestamp: Column<String> = text("timestamp")
    val result: Column<String> = text("result")
}
