package io.newm.server.database.migration

import io.newm.chain.util.extractStakeKeyHex
import io.newm.server.features.marketplace.database.MarketplaceSaleEntity
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V71__MarketplaceUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            exec("ALTER TABLE marketplace_sales ADD COLUMN IF NOT EXISTS owner_address_stake_key TEXT")
            MarketplaceSaleEntity.all().forEach { sale ->
                sale.ownerAddressStakeKey = sale.ownerAddress.extractStakeKeyHex()
            }
        }
    }
}
