package io.newm.server.config.database

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

object ConfigTable : IdTable<String>(name = "config") {
    override val id: Column<EntityID<String>> = text("id").entityId()
    val value: Column<String> = text("value")
}
