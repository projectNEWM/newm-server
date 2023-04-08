package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V13__CreateConfig : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    "CREATE TABLE IF NOT EXISTS config (id text PRIMARY KEY, value TEXT NOT NULL)",
                    "INSERT INTO config VALUES ('mint.price','6000000') ON CONFLICT(id) DO NOTHING"
                )
            )
        }
    }
}
