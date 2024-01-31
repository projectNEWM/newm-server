package io.newm.chain.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V4__CreateUsers : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    """
                    CREATE TABLE IF NOT EXISTS "api_users" ("id" BIGSERIAL PRIMARY KEY, "name" TEXT NOT NULL)
                    """.trimIndent(),
                    """
                    CREATE INDEX IF NOT EXISTS "api_users_name_index" ON "api_users" ("name")
                    """.trimIndent(),
                )
            )
        }
    }
}
