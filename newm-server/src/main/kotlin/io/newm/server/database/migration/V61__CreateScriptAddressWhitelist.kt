package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V61__CreateScriptAddressWhitelist : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    """
                    CREATE TABLE IF NOT EXISTS "script_address_whitelist" (id uuid PRIMARY KEY, "script_address" TEXT NOT NULL)
                    """.trimIndent(),
                    """
                    CREATE INDEX IF NOT EXISTS "script_address_whitelist_index" ON "script_address_whitelist" (script_address)
                    """.trimIndent(),
                ),
            )
        }
    }
}
