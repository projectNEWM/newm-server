package io.newm.chain.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V3__AlterLedgerUtxos : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    """
                        ALTER TABLE "ledger_utxos" ADD COLUMN "datum_hash" TEXT
                    """.trimIndent(),
                    """
                        ALTER TABLE "ledger_utxos" ADD COLUMN "datum" TEXT
                    """.trimIndent(),
                )
            )
        }
    }
}
