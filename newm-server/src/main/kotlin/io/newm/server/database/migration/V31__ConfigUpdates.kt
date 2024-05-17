package io.newm.server.database.migration

import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_EVEARA_STATUS_CHECK_MINUTES
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V31__ConfigUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            exec(
                // default 24 hours
                "INSERT INTO config VALUES ('$CONFIG_KEY_EVEARA_STATUS_CHECK_MINUTES','1440') ON CONFLICT(id) DO NOTHING",
            )
        }
    }
}
