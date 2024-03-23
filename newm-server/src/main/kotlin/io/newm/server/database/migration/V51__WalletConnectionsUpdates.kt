package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V51__WalletConnectionsUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            exec("ALTER TABLE wallet_connection_challenges DROP COLUMN IF EXISTS payload")
        }
    }
}
