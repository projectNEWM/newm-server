package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V62__ConfigUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            exec(
                // Add NEWM album id
                "INSERT INTO config VALUES ('newm.playlist.id','4I1cdKzkEzNotxwMPqtM6U') ON CONFLICT(id) DO NOTHING",
            )
        }
    }
}
