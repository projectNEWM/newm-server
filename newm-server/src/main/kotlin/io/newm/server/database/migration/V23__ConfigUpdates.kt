package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V23__ConfigUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    "INSERT INTO config VALUES ('scheduler.evearaSync','0 0 0 * * ?') ON CONFLICT(id) DO NOTHING"
                )
            )
        }
    }
}
