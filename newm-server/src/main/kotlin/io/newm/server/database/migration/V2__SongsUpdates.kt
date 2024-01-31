package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V2__SongsUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    """
                    UPDATE songs SET genre = '' WHERE genre IS NULL
                    """.trimIndent(),
                    """
                    ALTER TABLE songs
                        ALTER COLUMN genre SET NOT NULL
                    """.trimIndent(),
                    """
                    ALTER TABLE songs
                        ADD COLUMN minting_status int,
                        ADD COLUMN marketplace_status int
                    """.trimIndent()
                )
            )
        }
    }
}
