package io.newm.server.features.cardano.database

import java.util.UUID
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class ScriptAddressWhitelistEntity(
    id: EntityID<UUID>
) : UUIDEntity(id) {
    var scriptAddress: String by ScriptAddressWhitelistTable.scriptAddress

    companion object : UUIDEntityClass<ScriptAddressWhitelistEntity>(ScriptAddressWhitelistTable)
}
