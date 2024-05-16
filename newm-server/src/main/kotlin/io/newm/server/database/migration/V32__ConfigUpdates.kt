package io.newm.server.database.migration

import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_EVEARA_STATUS_CHECK_REFIRE
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V32__ConfigUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    // Check album status every 720(minutes) X 30(days) = 21600 minutes
                    "INSERT INTO config VALUES ('$CONFIG_KEY_EVEARA_STATUS_CHECK_REFIRE','21600') ON CONFLICT(id) DO NOTHING",
                )
            )
        }
    }
}
