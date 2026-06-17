package io.newm.server.config.database

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.Column

object ConfigTable : IdTable<String>(name = "config") {
    override val id: Column<EntityID<String>> = text("id").entityId()
    val value: Column<String> = text("value")
}
