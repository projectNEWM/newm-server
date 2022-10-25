package io.newm.chain.database.table

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column

object PaymentStakeAddressTable : LongIdTable(name = "payment_stake_addresses") {
    val receivingAddress: Column<String> = text("receiving_address")
    val stakeAddress: Column<String> = text("stake_address")
}
