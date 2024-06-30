package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V62__UserSongUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    "CREATE INDEX IF NOT EXISTS songs_title_index ON songs(title)",
                    "CREATE INDEX IF NOT EXISTS songs_description_index ON songs(description)",
                    "CREATE INDEX IF NOT EXISTS songs_nft_name_index ON songs(nftName)",
                    "CREATE INDEX IF NOT EXISTS users_nickname_index ON users(nickname)",
                    "CREATE INDEX IF NOT EXISTS users_first_name_index ON users(first_name)",
                    "CREATE INDEX IF NOT EXISTS users_last_name_index ON users(last_name)",
                )
            )
        }
    }
}
