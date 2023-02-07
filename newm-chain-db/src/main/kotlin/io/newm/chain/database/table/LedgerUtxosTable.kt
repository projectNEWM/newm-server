package io.newm.chain.database.table

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column

object LedgerUtxosTable : LongIdTable(name = "ledger_utxos") {
    // foreign key to the ledger table
    val ledgerId: Column<Long> = long("ledger_id").references(LedgerTable.id)

    // transaction id
    val txId: Column<String> = text("tx_id")

    // transaction index
    val txIx: Column<Int> = integer("tx_ix")

    // datum hash
    val datumHash: Column<String?> = text("datum_hash").nullable()

    // inline datum value
    val datum: Column<String?> = text("datum").nullable()

    // lovelaces in this utxo
    val lovelace: Column<String> = text("lovelace")

    // when this utxo was created or spent
    val blockCreated: Column<Long> = long("block_created")
    val slotCreated: Column<Long> = long("slot_created")
    val blockSpent: Column<Long?> = long("block_spent").nullable()
    val slotSpent: Column<Long?> = long("slot_spent").nullable()
    val transactionSpent: Column<String?> = text("transaction_spent").nullable()

    // the raw cbor of this utxo entry
    val cbor: Column<ByteArray?> = binary("cbor").nullable()
}
