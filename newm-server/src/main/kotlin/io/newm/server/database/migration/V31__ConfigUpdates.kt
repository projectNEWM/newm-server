package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V31__ConfigUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    // default 24 hours
                    "INSERT INTO config VALUES ('eveara.statusCheckMinutes','1440') ON CONFLICT(id) DO NOTHING",
                )
            )
        }
    }
}
