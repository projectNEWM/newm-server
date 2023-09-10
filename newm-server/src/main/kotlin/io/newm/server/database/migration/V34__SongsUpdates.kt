package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V34__SongsUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    "ALTER TABLE songs ADD COLUMN IF NOT EXISTS audio_encoding_status INT NOT NULL DEFAULT 0",
                    "UPDATE songs SET audio_encoding_status = 2 WHERE stream_url IS NOT NULL AND clip_url IS NOT NULL"
                )
            )
        }
    }
}
