package io.newm.chain.database.table

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column

object KeysTable : LongIdTable(name = "keys") {
    val skey: Column<String> = text("skey")
    val vkey: Column<String> = text("vkey")
    val address: Column<String> = text("address")
    val created: Column<Long> = long("created")
    val script: Column<String?> = text("script").nullable()
    val scriptAddress: Column<String?> = text("script_address").nullable()
}
