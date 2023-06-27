package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.SQLException

@Suppress("unused")
class V26__SongsUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            try {
                exec("ALTER TABLE songs RENAME COLUMN ipi TO ipis")
            } catch (e: SQLException) {
                println("Column 'ipi' doesn't exist")
            }
        }
    }
}
