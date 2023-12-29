package io.newm.chain.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V14__CleanupIndexes : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    // drop some indexes
                    """
                        DROP INDEX IF EXISTS "chain_created_index"
                    """.trimIndent(),
                    """
                        DROP INDEX IF EXISTS "chain_slot_number_index"
                    """.trimIndent(),
                    """
                        DROP INDEX IF EXISTS "chain_block_number_index"
                    """.trimIndent(),
                    """
                        DROP INDEX IF EXISTS "ledger_utxo_spent_index"
                    """.trimIndent(),
                    """
                        DROP INDEX IF EXISTS "ledger_utxo_slot_spent_index"
                    """.trimIndent(),

                    // drop unused columns
                    """
                        ALTER TABLE "chain" DROP COLUMN "created"
                    """.trimIndent(),
                    """
                        ALTER TABLE "chain" DROP COLUMN "processed"
                    """.trimIndent(),
                    """
                        ALTER TABLE "ledger_utxos" DROP COLUMN "slot_created"
                    """.trimIndent(),
                    """
                        ALTER TABLE "ledger_utxos" DROP COLUMN "slot_spent"
                    """.trimIndent(),

                    // re-create some indexes
                    """
                        CREATE UNIQUE INDEX IF NOT EXISTS "chain_slot_number_index" ON "chain" (slot_number)
                    """.trimIndent(),
                    """
                        CREATE UNIQUE INDEX IF NOT EXISTS "chain_block_number_index" ON "chain" (block_number)
                    """.trimIndent(),
                    """
                        CREATE INDEX IF NOT EXISTS "ledger_utxo_block_spent_index" ON "ledger_utxos" (block_spent)
                    """.trimIndent()
                )
            )
        }
    }
}
