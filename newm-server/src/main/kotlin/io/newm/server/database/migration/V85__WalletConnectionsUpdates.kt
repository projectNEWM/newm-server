package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V85__WalletConnectionsUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    "ALTER TABLE wallet_connections RENAME COLUMN stake_address TO address",
                    "ALTER TABLE wallet_connections ADD COLUMN IF NOT EXISTS chain int",
                    "UPDATE wallet_connections SET chain = 0 WHERE chain IS NULL",
                    "ALTER TABLE wallet_connections ALTER COLUMN chain SET NOT NULL",
                    "ALTER TABLE wallet_connections ADD COLUMN IF NOT EXISTS name TEXT",
                    "UPDATE wallet_connections SET name = 'Cardano-' || right(address, 6) WHERE name IS NULL",
                    "ALTER TABLE wallet_connections ALTER COLUMN name SET NOT NULL"
                )
            )
        }
    }
}
