package io.newm.server.database.migration

import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_EARNINGS_CLAIM_ORDER_FEE
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_EARNINGS_MONITOR_PAYMENT_ADDRESS_TIMEOUT_MIN
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V57__ConfigUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    // Expire a ClaimOrder after 30 minutes by default
                    "INSERT INTO config VALUES ('$CONFIG_KEY_EARNINGS_MONITOR_PAYMENT_ADDRESS_TIMEOUT_MIN','30') ON CONFLICT(id) DO NOTHING",
                    // ClaimOrder takes 2 ada by default
                    "INSERT INTO config VALUES ('$CONFIG_KEY_EARNINGS_CLAIM_ORDER_FEE','2000000') ON CONFLICT(id) DO NOTHING",
                    // update claim_orders table to add a new column
                    """ALTER TABLE claim_orders ADD COLUMN IF NOT EXISTS payment_address TEXT NOT NULL""",
                    """ALTER TABLE claim_orders ADD COLUMN IF NOT EXISTS payment_amount BIGINT NOT NULL""",
                    // indexes for earnings queries - this table could grow huge so we need indexes
                    """CREATE INDEX IF NOT EXISTS "earnings_song_id_created_idx" ON earnings (song_id, created_at DESC)""",
                    """CREATE INDEX IF NOT EXISTS "earnings_stake_address_claimed_created_idx" ON earnings (stake_address, claimed, created_at DESC)""",
                    // indexes for claim_orders queries - this table could grow huge so we need indexes
                    """CREATE INDEX IF NOT EXISTS "claim_orders_stake_address_status_idx" ON claim_orders (stake_address, status)""",
                )
            )
        }
    }
}
