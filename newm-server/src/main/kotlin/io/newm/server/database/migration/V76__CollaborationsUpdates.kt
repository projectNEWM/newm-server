package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V76__CollaborationsUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    "ALTER TABLE collaborations ADD COLUMN IF NOT EXISTS roles TEXT ARRAY",
                    "UPDATE collaborations SET roles = ARRAY[role] WHERE role IS NOT NULL",
                    "ALTER TABLE collaborations DROP COLUMN IF EXISTS role"
                )
            )
        }
    }
}
