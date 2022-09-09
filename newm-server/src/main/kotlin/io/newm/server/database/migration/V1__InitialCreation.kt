package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V1__InitialCreation : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    """
                        CREATE TABLE IF NOT EXISTS users
                        (
                            id uuid PRIMARY KEY,
                            oauth_type integer,
                            oauth_id text,
                            first_name text,
                            last_name text,
                            nickname text,
                            picture_url text,
                            role text,
                            genre text,
                            wallet_address text,
                            email text NOT NULL,
                            password_hash text
                        )
                    """.trimIndent(),
                    """
                        CREATE TABLE IF NOT EXISTS two_factor_auth
                        (
                            id bigserial PRIMARY KEY,
                            email text NOT NULL,
                            code_hash text NOT NULL,
                            expires_at timestamp without time zone NOT NULL
                        )
                    """.trimIndent(),
                    """
                        CREATE TABLE IF NOT EXISTS jwts
                        (
                            id uuid PRIMARY KEY,
                            user_id uuid NOT NULL,
                            expires_at timestamp without time zone NOT NULL,
                            CONSTRAINT fk_jwts_user_id__id FOREIGN KEY (user_id) REFERENCES users (id) ON UPDATE RESTRICT ON DELETE CASCADE
                        )
                    """.trimIndent(),
                    """
                        CREATE TABLE IF NOT EXISTS songs
                        (
                            id uuid PRIMARY KEY,
                            created_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            owner_id uuid NOT NULL,
                            title text NOT NULL,
                            genre text,
                            cover_art_url text,
                            description text,
                            credits text,
                            stream_url text,
                            nft_policy_id text,
                            nft_name text,
                            CONSTRAINT fk_songs_owner_id__id FOREIGN KEY (owner_id) REFERENCES users (id) ON UPDATE RESTRICT ON DELETE CASCADE
                        )
                    """.trimIndent(),
                    """
                        CREATE TABLE IF NOT EXISTS playlists
                        (
                            id uuid PRIMARY KEY,
                            created_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            owner_id uuid NOT NULL,
                            name text NOT NULL,
                            CONSTRAINT fk_playlists_owner_id__id FOREIGN KEY (owner_id) REFERENCES users (id) ON UPDATE RESTRICT ON DELETE CASCADE
                        )
                    """.trimIndent(),
                    """
                        CREATE TABLE IF NOT EXISTS songs_in_playlists
                        (
                            song_id uuid NOT NULL,
                            playlist_id uuid NOT NULL,
                            CONSTRAINT pk_songs_in_playlists PRIMARY KEY (song_id, playlist_id),
                            CONSTRAINT fk_songs_in_playlists_playlist_id__id FOREIGN KEY (playlist_id) REFERENCES playlists (id) ON UPDATE RESTRICT ON DELETE CASCADE,
                            CONSTRAINT fk_songs_in_playlists_song_id__id FOREIGN KEY (song_id) REFERENCES songs (id) ON UPDATE RESTRICT ON DELETE CASCADE
                        )
                    """.trimIndent()
                )
            )
        }
    }
}
