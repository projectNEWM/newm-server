package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V29__UsersUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    "ALTER TABLE users ADD COLUMN IF NOT EXISTS distribution_newm_participant_id BIGINT",
                    "ALTER TABLE collaborations ADD COLUMN IF NOT EXISTS distribution_artist_id BIGINT",
                )
            )
        }
    }
}
