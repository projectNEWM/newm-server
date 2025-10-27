package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V80__ConfigUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            exec("INSERT INTO config VALUES ('referralHero.rewardUsd','5000000') ON CONFLICT(id) DO NOTHING")
        }
    }
}
