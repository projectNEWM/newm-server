package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V52__CreateMarketplace : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    """
                    CREATE TABLE IF NOT EXISTS marketplace_sales (
                        id uuid PRIMARY KEY,
                        created_at timestamp without time zone NOT NULL,
                        status int NOT NULL,
                        song_id uuid NOT NULL,
                        owner_address text NOT NULL,
                        pointer_policy_id text NOT NULL,
                        pointer_asset_name text NOT NULL,
                        bundle_policy_id text NOT NULL,
                        bundle_asset_name text NOT NULL,
                        bundle_amount bigint NOT NULL,
                        cost_policy_id text NOT NULL,
                        cost_asset_name text NOT NULL,
                        cost_amount bigint NOT NULL,
                        max_bundle_size bigint NOT NULL,
                        total_bundle_quantity bigint NOT NULL,
                        available_bundle_quantity bigint NOT NULL,
                        CONSTRAINT fk_marketplace_sales_song_id__id FOREIGN KEY (song_id) REFERENCES songs (id) ON UPDATE RESTRICT ON DELETE RESTRICT
                    )
                    """.trimIndent(),
                    """
                    CREATE TABLE IF NOT EXISTS marketplace_purchases (
                        id uuid PRIMARY KEY,
                        created_at timestamp without time zone NOT NULL,
                        sale_id uuid NOT NULL,
                        bundle_quantity bigint NOT NULL,
                        CONSTRAINT fk_marketplace_purchases_sale_id__id FOREIGN KEY (sale_id) REFERENCES marketplace_sales (id) ON UPDATE RESTRICT ON DELETE RESTRICT
                    )
                    """.trimIndent(),
                    """
                    CREATE TABLE IF NOT EXISTS marketplace_bookmarks (
                        id text PRIMARY KEY,
                        txid TEXT NOT NULL,
                        block bigint NOT NULL,
                        slot bigint NOT NULL
                    )
                    """.trimIndent()
                )
            )
        }
    }
}
