package io.newm.server.features.cardano.database

import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.javatime.CurrentDateTime
import org.jetbrains.exposed.v1.javatime.datetime
import java.time.LocalDateTime

object KeyTable : UUIDTable(name = "keys") {
    val createdAt: Column<LocalDateTime> = datetime("created_at").defaultExpression(CurrentDateTime)
    val skey: Column<String> = text("skey")
    val vkey: Column<String> = text("vkey")
    val address: Column<String> = text("address")
    val script: Column<String?> = text("script").nullable()
    val scriptAddress: Column<String?> = text("script_address").nullable()
    val name: Column<String?> = text("name").nullable()
}
