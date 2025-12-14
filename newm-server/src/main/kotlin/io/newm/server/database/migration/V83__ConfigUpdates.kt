package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V83__ConfigUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            exec("INSERT INTO config VALUES ('paypal.itemizedInvoiceEnabled','true') ON CONFLICT(id) DO NOTHING")
        }
    }
}
