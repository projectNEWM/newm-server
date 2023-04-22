package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V15__SongsUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            exec(
                """
                ALTER TABLE songs
                    DROP COLUMN IF EXISTS credits,
                    ADD COLUMN IF NOT EXISTS album TEXT,
                    ADD COLUMN IF NOT EXISTS track INTEGER,
                    ADD COLUMN IF NOT EXISTS language TEXT,
                    ADD COLUMN IF NOT EXISTS copyright TEXT,
                    ADD COLUMN IF NOT EXISTS parental_advisory TEXT,
                    ADD COLUMN IF NOT EXISTS isrc TEXT,
                    ADD COLUMN IF NOT EXISTS iswc TEXT,
                    ADD COLUMN IF NOT EXISTS ipi TEXT ARRAY,
                    ADD COLUMN IF NOT EXISTS release_date DATE,
                    ADD COLUMN IF NOT EXISTS publication_date DATE,
                    ADD COLUMN IF NOT EXISTS lyrics_url TEXT,
                    ADD COLUMN IF NOT EXISTS token_agreement_url TEXT,
                    ADD COLUMN IF NOT EXISTS clip_url TEXT
                """.trimIndent()
            )
        }
    }
}
