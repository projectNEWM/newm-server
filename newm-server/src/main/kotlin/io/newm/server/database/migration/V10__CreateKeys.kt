package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V10__CreateKeys : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    // Keys
                    """CREATE TABLE IF NOT EXISTS "keys" ("id" uuid PRIMARY KEY, "created_at" timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP, "skey" TEXT NOT NULL, "vkey" TEXT NOT NULL, "address" TEXT NOT NULL, "script" TEXT, "script_address" TEXT)""",
                    """CREATE INDEX IF NOT EXISTS "keys_address_index" ON "keys" (address)""",
                    """CREATE INDEX IF NOT EXISTS "keys_script_address_index" ON "keys" (script_address ASC NULLS LAST)""",
                )
            )
        }
    }
}
