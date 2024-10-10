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
                    id uuid PRIMARY KEY,
                    time_stamp timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    minting_status int NOT NULL,
                    log_message text,
                    CONSTRAINT fk_minting_status_song_id__id FOREIGN KEY (song_id) REFERENCES songs (id) ON UPDATE RESTRICT ON DELETE CASCADE
                )
                """.trimIndent()
            )
        }
    }
}
