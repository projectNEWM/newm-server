package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V78__ReleasesUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    "ALTER TABLE releases ADD COLUMN IF NOT EXISTS mint_payment_type VARCHAR(255)",
                    "ALTER TABLE releases ADD COLUMN IF NOT EXISTS mint_cost BIGINT",
                    "UPDATE releases SET mint_cost = mint_cost_lovelace",
                    "UPDATE releases set mint_payment_type = 'ADA' where mint_cost IS NOT NULL",
                    "ALTER TABLE song_receipts ADD COLUMN IF NOT EXISTS newm_price BIGINT NOT NULL DEFAULT 0",
                    "ALTER TABLE song_receipts ADD COLUMN IF NOT EXISTS newm_dsp_price BIGINT NOT NULL DEFAULT 0",
                    "ALTER TABLE song_receipts ADD COLUMN IF NOT EXISTS newm_mint_price BIGINT NOT NULL DEFAULT 0",
                    "ALTER TABLE song_receipts ADD COLUMN IF NOT EXISTS newm_collab_price BIGINT NOT NULL DEFAULT 0",
                    "ALTER TABLE song_receipts ADD COLUMN IF NOT EXISTS usd_newm_exchange_rate BIGINT NOT NULL DEFAULT 0",
                )
            )
        }
    }
}
