package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V24__UsersUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    """
                    ALTER TABLE users
                        ADD COLUMN IF NOT EXISTS distribution_user_id UUID,
                        ADD COLUMN IF NOT EXISTS distribution_artist_id BIGINT,
                        ADD COLUMN IF NOT EXISTS distribution_participant_id BIGINT,
                        ADD COLUMN IF NOT EXISTS distribution_subscription_id BIGINT,
                        ADD COLUMN IF NOT EXISTS distribution_label_id BIGINT,
                        ADD COLUMN IF NOT EXISTS distribution_isni TEXT,
                        ADD COLUMN IF NOT EXISTS distribution_ipn TEXT
                    """.trimIndent(),
                    """
                    ALTER TABLE songs
                        ADD COLUMN IF NOT EXISTS distribution_track_id BIGINT
                    """.trimIndent(),
                    """
                    INSERT INTO config VALUES ('eveara.partnerSubscriptionId','570') ON CONFLICT(id) DO NOTHING
                    """.trimIndent()
                )
            )
        }
    }
}
