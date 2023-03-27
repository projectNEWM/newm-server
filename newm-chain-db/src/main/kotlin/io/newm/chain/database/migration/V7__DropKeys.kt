package io.newm.chain.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V7__DropKeys : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    """
                        DROP TABLE IF EXISTS "keys"
                    """.trimIndent(),
                    """
                        DROP TABLE IF EXISTS "transaction_dest_addresses"
                    """.trimIndent()
                )
            )
        }
    }
}
