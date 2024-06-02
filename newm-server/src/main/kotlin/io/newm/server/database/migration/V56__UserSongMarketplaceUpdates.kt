package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V56__UserSongMarketplaceUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    "CREATE INDEX IF NOT EXISTS users_created_at_index ON users(created_at)",
                    "CREATE INDEX IF NOT EXISTS songs_created_at_index ON songs(created_at)",
                    "CREATE INDEX IF NOT EXISTS songs_owner_id_index ON songs(owner_id)",
                    "CREATE INDEX IF NOT EXISTS marketplace_sales_created_at_index ON marketplace_sales(created_at)",
                    "CREATE INDEX IF NOT EXISTS marketplace_sales_song_id_index ON marketplace_sales(song_id)",
                    "CREATE INDEX IF NOT EXISTS marketplace_sales_pointer_policy_id_index ON marketplace_sales(pointer_policy_id)",
                    "CREATE INDEX IF NOT EXISTS marketplace_sales_pointer_asset_name_index ON marketplace_sales(pointer_asset_name)",
                    "CREATE INDEX IF NOT EXISTS marketplace_pending_orders_created_at_index ON marketplace_pending_orders(created_at)"
                )
            )
        }
    }
}
