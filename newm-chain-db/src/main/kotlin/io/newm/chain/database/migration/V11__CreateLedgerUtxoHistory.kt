package io.newm.chain.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V11__CreateLedgerUtxoHistory : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    """
                    CREATE TABLE IF NOT EXISTS "ledger_utxos_history" ("payment_cred" TEXT NOT NULL, "stake_cred" TEXT, "tx_id" TEXT NOT NULL, "block" BIGINT NOT NULL DEFAULT 0, PRIMARY KEY (payment_cred, tx_id))
                    """.trimIndent(),
                    """
                    CREATE INDEX IF NOT EXISTS "ledger_utxos_history_stake_cred_index" ON "ledger_utxos_history" (stake_cred)
                    """.trimIndent(),
                    """
                    CREATE INDEX IF NOT EXISTS "ledger_utxos_history_block_index" ON "ledger_utxos_history" (block)
                    """.trimIndent(),
                    """
                    ALTER TABLE "ledger_utxos" ADD COLUMN IF NOT EXISTS "payment_cred" TEXT
                    """.trimIndent(),
                    """
                    ALTER TABLE "ledger_utxos" ADD COLUMN IF NOT EXISTS "stake_cred" TEXT
                    """.trimIndent(),
                    """
                    CREATE INDEX IF NOT EXISTS "ledger_utxos_payment_cred_index" ON "ledger_utxos" (payment_cred)
                    """.trimIndent(),
                    """
                    CREATE INDEX IF NOT EXISTS "ledger_utxos_stake_cred_index" ON "ledger_utxos" (stake_cred)
                    """.trimIndent(),
                )
            )
        }
    }
}
