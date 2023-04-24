package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V17__CollaborationsUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            exec("ALTER TABLE collaborations ADD COLUMN IF NOT EXISTS credited boolean NOT NULL DEFAULT false")
        }
    }
}
