package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V4__SongsUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    """
                    UPDATE songs SET minting_status = 0 WHERE minting_status IS NULL
                    """.trimIndent(),
                    """
                    UPDATE songs SET marketplace_status = 0 WHERE marketplace_status IS NULL
                    """.trimIndent(),
                    """
                    ALTER TABLE songs
                        ALTER COLUMN minting_status SET NOT NULL,
                        ALTER COLUMN marketplace_status SET NOT NULL
                    """.trimIndent()
                )
            )
        }
    }
}
