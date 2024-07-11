package io.newm.server.database.migration

import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_NEWM_PLAYLIST_ID
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V62__ConfigUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            exec(
                // Add NEWM album id
                "INSERT INTO config VALUES ('$CONFIG_KEY_NEWM_PLAYLIST_ID','4I1cdKzkEzNotxwMPqtM6U') ON CONFLICT(id) DO NOTHING",
            )
        }
    }
}
