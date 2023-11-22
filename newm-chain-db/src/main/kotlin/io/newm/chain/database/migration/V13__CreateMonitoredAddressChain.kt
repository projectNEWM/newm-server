package io.newm.chain.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V13__CreateMonitoredAddressChain : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    """
                        CREATE TABLE IF NOT EXISTS "monitored_address_chain" ("id" BIGSERIAL PRIMARY KEY, "address" TEXT NOT NULL, "height" BIGINT NOT NULL, "slot" BIGINT NOT NULL, "hash" TEXT NOT NULL)
                    """.trimIndent(),
                    """
                        CREATE INDEX IF NOT EXISTS "monitored_address_chain_address_height_slot_index" ON "monitored_address_chain" (address,height,slot)
                    """.trimIndent(),
                )
            )
        }
    }
}
