package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V50__CreateWalletConnections : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    """
                    CREATE TABLE IF NOT EXISTS wallet_connection_challenges (
                        id uuid PRIMARY KEY,
                        created_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        method int NOT NULL,
                        stake_address text NOT NULL,
                        payload text NOT NULL
                    )
                    """.trimIndent(),
                    """
                    CREATE TABLE IF NOT EXISTS wallet_connections (
                        id uuid PRIMARY KEY,
                        created_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        stake_address text NOT NULL,
                        user_id uuid,
                        CONSTRAINT fk_wallet_connections_user_id__id FOREIGN KEY (user_id) REFERENCES users (id) ON UPDATE RESTRICT ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )
            )
        }
    }
}
