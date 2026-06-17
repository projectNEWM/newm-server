package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

@Suppress("unused")
class V27__ConfigUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    "INSERT INTO config VALUES ('eveara.newmEmail','accounting@newm.io') ON CONFLICT(id) DO NOTHING",
                    "ALTER TABLE songs ADD COLUMN IF NOT EXISTS distribution_release_id BIGINT",
                )
            )
        }
    }
}
