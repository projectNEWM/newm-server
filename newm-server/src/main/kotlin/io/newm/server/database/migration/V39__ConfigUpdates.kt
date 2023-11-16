package io.newm.server.database.migration

import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_DISTRIBUTION_PRICE_USD
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V39__ConfigUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    // default $14.99
                    "INSERT INTO config VALUES ('$CONFIG_KEY_DISTRIBUTION_PRICE_USD','14990000') ON CONFLICT(id) DO NOTHING",
                )
            )
        }
    }
}
