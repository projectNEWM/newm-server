package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V44__CascadeFixes : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    """
                    ALTER TABLE collaborations
                    DROP CONSTRAINT fk_collaborations_song_id__id,
                    ADD CONSTRAINT fk_collaborations_song_id__id FOREIGN KEY (song_id) REFERENCES songs (id) ON UPDATE RESTRICT ON DELETE NO ACTION
                    """.trimIndent(),
                    """
                    ALTER TABLE jwts
                    DROP CONSTRAINT fk_jwts_user_id__id,
                    ADD CONSTRAINT fk_jwts_user_id__id FOREIGN KEY (user_id) REFERENCES users (id) ON UPDATE RESTRICT ON DELETE NO ACTION
                    """.trimIndent(),
                    """
                    ALTER TABLE playlists
                    DROP CONSTRAINT fk_playlists_owner_id__id,
                    ADD CONSTRAINT fk_playlists_owner_id__id FOREIGN KEY (owner_id) REFERENCES users (id) ON UPDATE RESTRICT ON DELETE NO ACTION
                    """.trimIndent(),
                    """
                    ALTER TABLE song_receipts
                    DROP CONSTRAINT fk_song_receipts_song_id__id,
                    ADD CONSTRAINT fk_song_receipts_song_id__id FOREIGN KEY (song_id) REFERENCES songs (id) ON UPDATE RESTRICT ON DELETE NO ACTION
                    """.trimIndent(),
                    """
                    ALTER TABLE songs
                    DROP CONSTRAINT fk_songs_owner_id__id,
                    ADD CONSTRAINT fk_songs_owner_id__id FOREIGN KEY (owner_id) REFERENCES users (id) ON UPDATE RESTRICT ON DELETE NO ACTION
                    """.trimIndent(),
                    """
                    ALTER TABLE songs
                    DROP CONSTRAINT fk_songs_payment_key_id__id,
                    ADD CONSTRAINT fk_songs_payment_key_id__id FOREIGN KEY (payment_key_id) REFERENCES keys (id) ON UPDATE RESTRICT ON DELETE NO ACTION
                    """.trimIndent(),
                    """
                    ALTER TABLE songs_in_playlists
                    DROP CONSTRAINT fk_songs_in_playlists_playlist_id__id,
                    ADD CONSTRAINT fk_songs_in_playlists_playlist_id__id FOREIGN KEY (playlist_id) REFERENCES playlists (id) ON UPDATE NO ACTION ON DELETE NO ACTION
                    """.trimIndent(),
                    """
                    ALTER TABLE songs_in_playlists
                    DROP CONSTRAINT fk_songs_in_playlists_song_id__id,
                    ADD CONSTRAINT fk_songs_in_playlists_song_id__id FOREIGN KEY (song_id) REFERENCES songs (id) ON UPDATE NO ACTION ON DELETE NO ACTION
                    """.trimIndent(),
                )
            )
        }
    }
}
