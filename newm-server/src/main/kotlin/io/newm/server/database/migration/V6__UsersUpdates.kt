package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V6__UsersUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    """
                    ALTER TABLE users ADD COLUMN IF NOT EXISTS verification_status int
                    """.trimIndent(),
                    """
                    UPDATE users SET verification_status = 0 WHERE verification_status IS NULL
                    """.trimIndent(),
                    """
                    ALTER TABLE users ALTER COLUMN verification_status SET NOT NULL
                    """.trimIndent()
                )
            )
        }
    }
}
