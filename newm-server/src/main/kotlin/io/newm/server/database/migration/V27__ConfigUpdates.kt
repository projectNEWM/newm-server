package io.newm.server.database.migration

import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_EVEARA_NEWM_EMAIL
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V27__ConfigUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    "INSERT INTO config VALUES ('$CONFIG_KEY_EVEARA_NEWM_EMAIL','accounting@newm.io') ON CONFLICT(id) DO NOTHING",
                    "ALTER TABLE songs ADD COLUMN IF NOT EXISTS distribution_release_id BIGINT",
                )
            )
        }
    }
}
