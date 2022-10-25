package io.newm.chain.database.table

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column

object StakeRegistrationsTable : LongIdTable(name = "stake_registrations") {

    // the user's stake address
    val stakeAddress: Column<String> = text("stake_address")

    // the slot where the stake key registration happened
    val slot: Column<Long> = long("slot")

    // the transaction index inside the block where the stake key registration happened
    val txIndex: Column<Int> = integer("tx_index")

    // the certificate index inside the transaction where the stake key registration happened
    val certIndex: Column<Int> = integer("cert_index")
}
