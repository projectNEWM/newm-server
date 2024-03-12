package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V49__SongsUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    "ALTER TABLE songs ADD COLUMN IF NOT EXISTS has_submitted_for_distribution BOOLEAN NOT NULL DEFAULT FALSE",
                    "UPDATE songs SET has_submitted_for_distribution = TRUE WHERE minting_status in (8,9,10,11,12,16,17,18,19,20)",
                )
            )
        }
    }
}
