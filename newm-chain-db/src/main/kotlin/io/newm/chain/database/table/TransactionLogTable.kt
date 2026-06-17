package io.newm.chain.database.table

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object TransactionLogTable : LongIdTable(name = "transaction_log") {
    val transactionId: Column<String> = text("transaction_id")
    val cbor: Column<String> = text("cbor")
    val timestamp: Column<String> = text("timestamp")
    val result: Column<String> = text("result")
}
