package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V65__JwtsDrop : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            exec("DROP TABLE IF EXISTS jwts")
        }
    }
}
