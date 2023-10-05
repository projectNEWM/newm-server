package io.newm.chain.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V12__AlterNativeAssetLogAddressTxLog : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    """
                        ALTER TABLE "address_tx_log" ADD COLUMN "block_number" BIGINT
                    """.trimIndent(),
                    """
                        ALTER TABLE "native_asset_log" ADD COLUMN "block_number" BIGINT
                    """.trimIndent(),
                    """
                        CREATE INDEX IF NOT EXISTS "address_tx_log_block_number_index" ON "address_tx_log" ("block_number")
                    """.trimIndent(),
                    """
                        CREATE INDEX IF NOT EXISTS "native_asset_log_block_number_index" ON "native_asset_log" ("block_number")
                    """.trimIndent(),
                )
            )
        }
    }
}
