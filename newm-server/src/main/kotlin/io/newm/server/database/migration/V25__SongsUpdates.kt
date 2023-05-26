package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V25__SongsUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            exec(
                """
                ALTER TABLE songs
                    ALTER COLUMN arweave_lyrics_url TYPE TEXT,
                    ADD COLUMN IF NOT EXISTS barcode_type INTEGER,
                    ADD COLUMN IF NOT EXISTS barcode_number TEXT
                """.trimIndent()
            )
        }
    }
}
