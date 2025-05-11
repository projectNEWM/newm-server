package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V55__ConfigUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            exec(
                // Check album status every 720(minutes) X 30(days) = 21600 minutes
                "INSERT INTO config VALUES ('eveara.statusCheckDeclinedMaxRefire','21600') ON CONFLICT(id) DO NOTHING",
            )
        }
    }
}
