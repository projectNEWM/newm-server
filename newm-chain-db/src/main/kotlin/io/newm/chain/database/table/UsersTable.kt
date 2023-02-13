package io.newm.chain.database.table

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column

object UsersTable : LongIdTable(name = "api_users") {
    val name: Column<String> = text("name")
}
