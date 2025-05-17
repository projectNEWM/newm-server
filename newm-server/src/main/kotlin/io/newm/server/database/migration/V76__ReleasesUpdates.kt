package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V76__ReleasesUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    "ALTER TABLE releases ADD COLUMN IF NOT EXISTS mint_payment_type VARCHAR(255)",
                    "ALTER TABLE releases ADD COLUMN IF NOT EXISTS mint_cost BIGINT",
                    "UPDATE releases SET mint_cost = mint_cost_lovelace",
                    "UPDATE releases set mint_payment_type = 'ADA' where mint_cost IS NOT NULL",
                )
            )
        }
    }
}
