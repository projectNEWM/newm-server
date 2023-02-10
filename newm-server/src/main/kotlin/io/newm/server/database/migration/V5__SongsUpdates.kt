package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V5__SongsUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    """
                        ALTER TABLE songs ADD COLUMN IF NOT EXISTS genres TEXT ARRAY
                    """.trimIndent(),
                    """
                        UPDATE songs SET genres[1] = genre
                    """.trimIndent(),
                    """
                        ALTER TABLE songs DROP COLUMN genre
                    """.trimIndent()
                )
            )
        }
    }
}
