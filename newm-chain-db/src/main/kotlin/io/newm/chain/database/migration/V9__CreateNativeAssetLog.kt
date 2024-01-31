package io.newm.chain.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V9__CreateNativeAssetLog : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    """
                    CREATE TABLE IF NOT EXISTS "native_asset_log" ("id" BIGSERIAL PRIMARY KEY, "monitor_native_assets_response" BYTEA NOT NULL)
                    """.trimIndent(),
                )
            )
        }
    }
}
