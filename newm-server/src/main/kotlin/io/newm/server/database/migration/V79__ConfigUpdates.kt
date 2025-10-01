package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V79__ConfigUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            exec("INSERT INTO config VALUES ('mint.cip68UtxoReference','<update_me>') ON CONFLICT(id) DO NOTHING")
        }
    }
}
