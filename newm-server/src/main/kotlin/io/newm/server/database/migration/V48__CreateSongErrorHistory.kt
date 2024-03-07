package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V48__CreateSongErrorHistory : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            exec(
                """
                CREATE TABLE IF NOT EXISTS song_error_history (
                    id uuid PRIMARY KEY,
                    created_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    song_id uuid NOT NULL,
                    error_message text NOT NULL,
                    CONSTRAINT fk_song_error_history_song_id__id FOREIGN KEY (song_id) REFERENCES songs (id) ON UPDATE RESTRICT ON DELETE RESTRICT
                )
                """.trimIndent()
            )
        }
    }
}
