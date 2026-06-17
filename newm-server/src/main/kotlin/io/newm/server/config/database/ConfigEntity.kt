package io.newm.server.config.database

import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class ConfigEntity(
    id: EntityID<String>
) : Entity<String>(id) {
    var value: String by ConfigTable.value

    companion object : EntityClass<String, ConfigEntity>(ConfigTable)
}
