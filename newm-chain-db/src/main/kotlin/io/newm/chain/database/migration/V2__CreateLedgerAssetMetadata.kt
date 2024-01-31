package io.newm.chain.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V2__CreateLedgerAssetMetadata : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    """
                    CREATE TABLE IF NOT EXISTS "ledger_asset_metadata" ("id" BIGSERIAL PRIMARY KEY, "asset_id" BIGINT NOT NULL, "key_type" TEXT NOT NULL, "key" TEXT NOT NULL, "value_type" TEXT NOT NULL, "value" TEXT NOT NULL, "nest_level" INTEGER NOT NULL, "parent_id" BIGINT)
                    """.trimIndent(),
                    """
                    CREATE INDEX IF NOT EXISTS "ledger_asset_metadata_asset_id_key_value_nest_level_index" ON "ledger_asset_metadata" ("asset_id","key","value","nest_level")
                    """.trimIndent(),
                )
            )
        }
    }
}
