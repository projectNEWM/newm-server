package io.newm.chain.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V10__AlterLedgerAssetMetadata : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    """
                        DROP INDEX IF EXISTS "ledger_asset_metadata_asset_id_key_value_nest_level_index"
                    """.trimIndent(),
                    """
                        CREATE INDEX IF NOT EXISTS "ledger_asset_metadata_asset_id_parent_id" ON "ledger_asset_metadata" ("asset_id","parent_id")
                    """.trimIndent(),
                )
            )
        }
    }
}
