package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V67__SongsReleasesUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    "ALTER TABLE releases ADD COLUMN IF NOT EXISTS mint_cost_lovelace BIGINT",
                    "UPDATE releases SET mint_cost_lovelace = songs.mint_cost_lovelace FROM songs WHERE songs.release_id = releases.id",
                    "ALTER TABLE songs DROP COLUMN IF EXISTS mint_cost_lovelace"
                )
            )
        }
    }
}
