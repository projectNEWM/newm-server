package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V11__SongsUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            exec("ALTER TABLE songs ADD COLUMN IF NOT EXISTS payment_key_id uuid CONSTRAINT fk_songs_payment_key_id__id REFERENCES keys (id) ON DELETE NO ACTION ON UPDATE RESTRICT")
        }
    }
}
