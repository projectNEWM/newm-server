package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V18__ConfigUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    // keep 10 ada in the cash register even when we collect
                    "INSERT INTO config VALUES ('mint.cashRegisterMinAmount','10000000') ON CONFLICT(id) DO NOTHING",
                    // collect cash register to moneybox if it will go over 100 Ada + 10 Ada
                    "INSERT INTO config VALUES ('mint.cashRegisterCollectionAmount','100000000') ON CONFLICT(id) DO NOTHING",
                    "ALTER TABLE users ADD COLUMN IF NOT EXISTS admin BOOLEAN NOT NULL DEFAULT false",
                    "ALTER TABLE keys ADD COLUMN IF NOT EXISTS name TEXT",
                    "CREATE INDEX IF NOT EXISTS users_name_index ON keys (name)",
                )
            )
        }
    }
}
