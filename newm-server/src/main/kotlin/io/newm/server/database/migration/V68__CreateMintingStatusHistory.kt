package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V68__CreateMintingStatusHistory : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            exec(
                """
                CREATE TABLE IF NOT EXISTS minting_status_history (
                    id uuid PRIMARY KEY NOT NULL,
                    song_id uuid NOT NULL,
                    created_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    minting_status text NOT NULL,
                    log_message text
                )
                """.trimIndent()
            )
        }
    }
}
