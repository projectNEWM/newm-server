package io.newm.chain.database.table

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column

object StakeDelegationsTable : LongIdTable(name = "stake_delegations") {
    // the block in which this delegation/deregistration occurred
    val blockNumber: Column<Long> = long("block_number")

    // the user's stake address
    val stakeAddress: Column<String> = text("stake_address")

    // the epoch when the user last dripped this token
    val epoch: Column<Long> = long("epoch")

    // the pool id the user delegated to. null if this is a de-registration of the stake address
    val poolId: Column<String?> = text("pool_id").nullable()
}
