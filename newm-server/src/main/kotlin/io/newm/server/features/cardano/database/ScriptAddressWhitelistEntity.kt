package io.newm.server.features.cardano.database

import java.util.UUID
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class ScriptAddressWhitelistEntity(
    id: EntityID<UUID>
) : UUIDEntity(id) {
    var scriptAddress: String by ScriptAddressWhitelistTable.scriptAddress

    companion object : UUIDEntityClass<ScriptAddressWhitelistEntity>(ScriptAddressWhitelistTable)
}
