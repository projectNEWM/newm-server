package io.newm.chain.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V15__CleanupIndexes : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    """
                    CREATE INDEX IF NOT EXISTS "ledger_stake_address_index" ON "ledger" (stake_address)
                    """.trimIndent(),
                )
            )
        }
    }
}
