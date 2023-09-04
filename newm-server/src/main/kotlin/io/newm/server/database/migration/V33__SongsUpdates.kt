package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V33__SongsUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            exec(
                """
                ALTER TABLE songs
                    DROP COLUMN IF EXISTS copyright,
                    ADD COLUMN IF NOT EXISTS comp_copyright_owner TEXT,
                    ADD COLUMN IF NOT EXISTS comp_copyright_year INTEGER,
                    ADD COLUMN IF NOT EXISTS phono_copyright_owner TEXT,
                    ADD COLUMN IF NOT EXISTS phono_copyright_year INTEGER
                """.trimIndent()
            )
        }
    }
}
