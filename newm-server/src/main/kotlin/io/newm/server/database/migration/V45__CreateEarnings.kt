package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V45__CreateEarnings : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    """
                    CREATE TABLE IF NOT EXISTS earnings
                    (
                        id uuid PRIMARY KEY,
                        song_id uuid,
                        stake_address text NOT NULL,
                        amount bigint NOT NULL,
                        memo text NOT NULL,
                        start_date timestamp without time zone,
                        end_date timestamp without time zone,
                        claimed boolean NOT NULL DEFAULT FALSE,
                        claimed_at timestamp without time zone,
                        claim_order_id uuid,
                        created_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        CONSTRAINT fk_earnings_song_id__id FOREIGN KEY (song_id) REFERENCES songs (id) ON UPDATE RESTRICT ON DELETE RESTRICT
                    )
                    """.trimIndent(),
                    """
                    CREATE TABLE IF NOT EXISTS claim_orders
                    (
                        id uuid PRIMARY KEY,
                        stake_address text NOT NULL,
                        key_id uuid NOT NULL,
                        status text NOT NULL,
                        earnings_ids uuid ARRAY NOT NULL,
                        failed_earnings_ids uuid ARRAY,
                        transaction_id text,
                        created_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        CONSTRAINT fk_claim_orders_key_id__id FOREIGN KEY (key_id) REFERENCES keys (id) ON UPDATE RESTRICT ON DELETE RESTRICT
                    )
                    """.trimIndent(),
                )
            )
        }
    }
}
