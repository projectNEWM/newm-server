package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V60__EarningsUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            // add error_message column to claim_orders table
            exec("ALTER TABLE claim_orders ADD COLUMN IF NOT EXISTS error_message TEXT")
        }
    }
}
