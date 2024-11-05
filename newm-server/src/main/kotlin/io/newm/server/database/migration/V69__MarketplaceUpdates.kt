package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V69__MarketplaceUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            exec(
                """
                CREATE TABLE IF NOT EXISTS marketplace_sale_owners (
                    id uuid PRIMARY KEY,
                    created_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    pointer_policy_id text NOT NULL,
                    pointer_asset_name text NOT NULL,
                    email text NOT NULL
                )
                """.trimIndent()
            )
        }
    }
}
