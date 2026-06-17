package io.newm.chain.database.table

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object PaymentStakeAddressTable : LongIdTable(name = "payment_stake_addresses") {
    val receivingAddress: Column<String> = text("receiving_address")
    val stakeAddress: Column<String> = text("stake_address")
}
