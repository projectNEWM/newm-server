package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V46__SongsUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    "ALTER TABLE songs ADD COLUMN IF NOT EXISTS instrumental BOOLEAN NOT NULL DEFAULT FALSE",
                    "UPDATE songs SET instrumental = TRUE WHERE 'Instrumental' = ANY(genres)",
                )
            )
        }
    }
}
