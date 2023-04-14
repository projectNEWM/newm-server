package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V14__CreateCollaborations : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            exec(
                """
                CREATE TABLE IF NOT EXISTS collaborations
                (
                    id uuid PRIMARY KEY,
                    created_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    song_id uuid NOT NULL,
                    email text NOT NULL,
                    role text,
                    royalty_rate float,
                    accepted boolean NOT NULL DEFAULT false,
                    CONSTRAINT fk_collaborations_song_id__id FOREIGN KEY (song_id) REFERENCES songs (id) ON UPDATE RESTRICT ON DELETE CASCADE
                )
                """.trimIndent()
            )
        }
    }
}
