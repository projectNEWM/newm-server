package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V41__CreateSongReceipts : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            exec(
                """
                CREATE TABLE IF NOT EXISTS song_receipts
                (
                    id uuid PRIMARY KEY,
                    created_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    song_id uuid NOT NULL,
                    ada_price bigint NOT NULL,
                    usd_price bigint NOT NULL,
                    ada_dsp_price bigint NOT NULL,
                    usd_dsp_price bigint NOT NULL,
                    ada_mint_price bigint NOT NULL,
                    usd_mint_price bigint NOT NULL,
                    ada_collab_price bigint NOT NULL,
                    usd_collab_price bigint NOT NULL,
                    usd_ada_exchange_rate bigint NOT NULL,
                    CONSTRAINT fk_song_receipts_song_id__id FOREIGN KEY (song_id) REFERENCES songs (id) ON UPDATE RESTRICT ON DELETE CASCADE
                )
                """.trimIndent()
            )
        }
    }
}
