package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V82__UsersUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    // Add column without default, allowing NULL temporarily
                    "ALTER TABLE users ADD COLUMN last_login TIMESTAMP",
                    // Backfill existing rows with their created_at value
                    "UPDATE users SET last_login = created_at WHERE last_login IS NULL",
                    // Set NOT NULL constraint and default for future inserts
                    "ALTER TABLE users ALTER COLUMN last_login SET NOT NULL",
                    "ALTER TABLE users ALTER COLUMN last_login SET DEFAULT CURRENT_TIMESTAMP"
                )
            )
        }
    }
}
