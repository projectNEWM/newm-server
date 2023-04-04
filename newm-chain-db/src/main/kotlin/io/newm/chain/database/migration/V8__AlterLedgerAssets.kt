package io.newm.chain.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V8__AlterLedgerAssets : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    """
                        ALTER TABLE "ledger_assets" DROP COLUMN IF EXISTS "image"
                    """.trimIndent(),
                    """
                        ALTER TABLE "ledger_assets" DROP COLUMN IF EXISTS "description"
                    """.trimIndent(),
                    """
                        ALTER TABLE "ledger_assets" ADD COLUMN IF NOT EXISTS "supply" TEXT DEFAULT '0'
                    """.trimIndent(),
                )
            )
        }
    }
}
