package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V37__UsersUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            exec(
                """
                    ALTER TABLE users
                        ADD COLUMN IF NOT EXISTS spotify_profile text,
                        ADD COLUMN IF NOT EXISTS sound_cloud_profile text,
                        ADD COLUMN IF NOT EXISTS apple_music_profile text
                """.trimIndent()
            )
        }
    }
}
