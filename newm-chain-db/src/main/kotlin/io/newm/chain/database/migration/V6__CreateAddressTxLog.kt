package io.newm.chain.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V6__CreateAddressTxLog : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    """
                        CREATE TABLE IF NOT EXISTS "address_tx_log" ("id" BIGSERIAL PRIMARY KEY, "address" TEXT NOT NULL, "tx_id" TEXT NOT NULL, "monitor_address_response" BYTEA NOT NULL)
                    """.trimIndent(),
                    """
                        CREATE INDEX IF NOT EXISTS "address_tx_log_address_tx_id_index" ON "address_tx_log" ("address","tx_id")
                    """.trimIndent(),
                )
            )
        }
    }
}
