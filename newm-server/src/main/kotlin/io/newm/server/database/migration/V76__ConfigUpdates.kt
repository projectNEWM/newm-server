package io.newm.server.database.migration

import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_REFERRAL_HERO_ENABLED
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V76__ConfigUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            exec("INSERT INTO config VALUES ('$CONFIG_KEY_REFERRAL_HERO_ENABLED','false') ON CONFLICT(id) DO NOTHING")
        }
    }
}
