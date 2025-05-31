package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V39__ConfigUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    // default $14.99
                    "INSERT INTO config VALUES ('distribution.price.usd','14990000') ON CONFLICT(id) DO NOTHING",
                )
            )
        }
    }
}
