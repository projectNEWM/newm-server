package io.newm.server.config.database

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID

class ConfigEntity(id: EntityID<String>) : Entity<String>(id) {
    val value: String by ConfigTable.value

    companion object : EntityClass<String, ConfigEntity>(ConfigTable)
}
