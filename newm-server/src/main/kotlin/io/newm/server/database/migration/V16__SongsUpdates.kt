package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V16__SongsUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            exec(
                """
                ALTER TABLE songs
                    ADD COLUMN IF NOT EXISTS arweave_cover_art_url TEXT,
                    ADD COLUMN IF NOT EXISTS arweave_lyrics_url INTEGER,
                    ADD COLUMN IF NOT EXISTS arweave_token_agreement_url TEXT,
                    ADD COLUMN IF NOT EXISTS arweave_clip_url TEXT
                """.trimIndent()
            )
        }
    }
}
