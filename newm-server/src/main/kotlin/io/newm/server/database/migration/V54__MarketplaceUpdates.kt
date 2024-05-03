package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V54__MarketplaceUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            exec(
                """
                CREATE TABLE IF NOT EXISTS marketplace_pending_orders (
                    id uuid PRIMARY KEY,
                    created_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    sale_id uuid NOT NULL,
                    bundle_quantity bigint NOT NULL,
                    incentive_amount bigint NOT NULL,
                    CONSTRAINT fk_marketplace_pending_orders_sale_id__id FOREIGN KEY (sale_id) REFERENCES marketplace_sales (id) ON UPDATE RESTRICT ON DELETE RESTRICT
                )
                """.trimIndent()
            )
        }
    }
}
