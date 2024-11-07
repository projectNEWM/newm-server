package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V70__MarketplaceUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    "DELETE FROM marketplace_pending_orders",
                    "ALTER TABLE marketplace_pending_orders ADD COLUMN IF NOT EXISTS service_fee_amount TEXT NOT NULL",
                    "DELETE FROM config WHERE id = 'marketplace.usdPriceAdjustmentFactor'",
                    "INSERT INTO config VALUES ('marketplace.usdPriceAdjustmentPercentage','2.5') ON CONFLICT(id) DO NOTHING",
                    "INSERT INTO config VALUES ('marketplace.serviceFeePercentage','10.0') ON CONFLICT(id) DO NOTHING"
                )
            )
        }
    }
}
