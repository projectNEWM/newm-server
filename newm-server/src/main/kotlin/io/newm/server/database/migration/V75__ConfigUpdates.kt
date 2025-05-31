package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V75__ConfigUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    // default $11.99 which is 20% discounted from $14.99 when the user pays USD value in $NEWM
                    "INSERT INTO config VALUES ('distribution.price.usd.newm','11990000') ON CONFLICT(id) DO NOTHING"
                )
            )
        }
    }
}
