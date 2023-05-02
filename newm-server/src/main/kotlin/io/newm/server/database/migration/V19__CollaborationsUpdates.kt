package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V19__CollaborationsUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            exec(
                """
                ALTER TABLE collaborations
                    DROP COLUMN IF EXISTS accepted,
                    ADD COLUMN IF NOT EXISTS status int NOT NULL DEFAULT 0
                """.trimIndent()
            )
        }
    }
}
