package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V64__ConfigUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    "DELETE FROM config WHERE id = 'marketplace.monitoringEnabled'",
                    "INSERT INTO config VALUES ('marketplace.monitoringMultiModeEnabled','false') ON CONFLICT(id) DO NOTHING"
                )
            )
        }
    }
}
