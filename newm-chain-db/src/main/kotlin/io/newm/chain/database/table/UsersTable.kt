package io.newm.chain.database.table

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object UsersTable : LongIdTable(name = "api_users") {
    val name: Column<String> = text("name")
}
