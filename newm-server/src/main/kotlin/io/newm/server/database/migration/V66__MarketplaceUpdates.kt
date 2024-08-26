package io.newm.server.database.migration

import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MARKETPLACE_PROFIT_AMOUNT_USD
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MARKETPLACE_USD_POLICY_ID
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MARKETPLACE_USD_PRICE_ADJUSTMENT_FACTOR
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
                    "INSERT INTO config VALUES ('${CONFIG_KEY_MARKETPLACE_USD_POLICY_ID}','555344') ON CONFLICT(id) DO NOTHING",
                    "INSERT INTO config VALUES ('${CONFIG_KEY_MARKETPLACE_USD_PRICE_ADJUSTMENT_FACTOR}','40') ON CONFLICT(id) DO NOTHING",
                    "INSERT INTO config VALUES ('$CONFIG_KEY_MARKETPLACE_PROFIT_AMOUNT_USD','500000000000') ON CONFLICT(id) DO NOTHING"
                )
            )
        }
    }
}
