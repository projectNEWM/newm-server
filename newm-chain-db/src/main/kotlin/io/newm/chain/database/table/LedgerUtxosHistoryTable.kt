package io.newm.chain.database.table

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table

object LedgerUtxosHistoryTable : Table(name = "ledger_utxos_history") {
    // credential
    val paymentCred: Column<String> = text("payment_cred")
    val stakeCred: Column<String?> = text("stake_cred").nullable()

    // transaction id
    val txId: Column<String> = text("tx_id")

    // the block number
    val block: Column<Long> = long("block")
}
