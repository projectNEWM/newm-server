package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V66__MarketplaceUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    "DELETE FROM marketplace_pending_orders",
                    "ALTER TABLE marketplace_pending_orders ADD COLUMN IF NOT EXISTS currency_amount TEXT NOT NULL",
                    "INSERT INTO config VALUES ('marketplace.usdPolicyId','555344') ON CONFLICT(id) DO NOTHING",
                    "INSERT INTO config VALUES ('marketplace.usdPriceAdjustmentFactor','40') ON CONFLICT(id) DO NOTHING",
                    "INSERT INTO config VALUES ('marketplace.profitAmountUsd','500000000000') ON CONFLICT(id) DO NOTHING"
                )
            )
        }
    }
}
