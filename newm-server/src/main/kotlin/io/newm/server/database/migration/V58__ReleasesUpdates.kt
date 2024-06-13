package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

class V58__ReleasesUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            exec(
                """
                ALTER TABLE releases
                    ADD COLUMN IF NOT EXISTS pre_save_page text
                """.trimIndent()
            )
        }
    }
}
