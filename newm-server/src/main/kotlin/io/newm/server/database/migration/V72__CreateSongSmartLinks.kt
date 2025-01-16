package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V72__CreateSongSmartLinks : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    """
                    CREATE TABLE IF NOT EXISTS song_smart_links (
                        id uuid PRIMARY KEY,
                        created_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        song_id uuid NOT NULL,
                        store_name TEXT NOT NULL,
                        url TEXT NOT NULL,
                        CONSTRAINT fk_song_smart_links_song_id__id FOREIGN KEY (song_id) REFERENCES songs(id) ON DELETE NO ACTION
                    )
                    """.trimIndent(),
                    "CREATE INDEX IF NOT EXISTS song_smart_links_song_id_index ON song_smart_links(song_id)",
                    "INSERT INTO config VALUES ('songSmartLinks.cacheTimeToLive','172800') ON CONFLICT(id) DO NOTHING",
                ),
            )
        }
    }
}
