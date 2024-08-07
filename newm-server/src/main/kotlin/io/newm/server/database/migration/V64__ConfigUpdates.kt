package io.newm.server.database.migration

import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MARKETPLACE_MONITORING_MULTI_MODE_ENABLED
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V64__ConfigUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    "DELETE FROM config WHERE id = 'marketplace.monitoringEnabled'",
                    "INSERT INTO config VALUES ('$CONFIG_KEY_MARKETPLACE_MONITORING_MULTI_MODE_ENABLED','false') ON CONFLICT(id) DO NOTHING"
                )
            )
        }
    }
}
