package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V74__UsersUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            exec(
                """
                ALTER TABLE users
                    ADD COLUMN IF NOT EXISTS referral_status int NOT NULL DEFAULT 0,
                    ADD COLUMN IF NOT EXISTS referral_code TEXT
                """.trimIndent()
            )
        }
    }
}
