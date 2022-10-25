package io.newm.chain.database.table

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column

object TransactionDestAddressTable : LongIdTable(name = "transaction_dest_addresses") {
    val address: Column<String> = text("address").index(customIndexName = "addresses_address_index", isUnique = false)
    val chainId: Column<EntityID<Long>> = reference("chain_id", ChainTable.id)
    val processed: Column<Boolean> =
        bool("processed").index(customIndexName = "addresses_processed_index", isUnique = false)
}
