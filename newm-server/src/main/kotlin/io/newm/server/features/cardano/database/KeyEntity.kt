package io.newm.server.features.cardano.database

import io.newm.chain.util.hexToByteArray
import io.newm.server.features.cardano.model.Key
import io.newm.server.features.cardano.model.KeyFilters
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.AndOp
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.less
import java.time.LocalDateTime
import java.util.*

class KeyEntity(
    id: EntityID<UUID>
) : UUIDEntity(id) {
    val createdAt: LocalDateTime by KeyTable.createdAt
    var skey: String by KeyTable.skey
    var vkey: String by KeyTable.vkey
    var address: String by KeyTable.address
    var script: String? by KeyTable.script
    var scriptAddress: String? by KeyTable.scriptAddress
    var name: String? by KeyTable.name

    fun toModel(skeyBytes: ByteArray): Key =
        Key(
            id = id.value,
            createdAt = createdAt,
            skey = skeyBytes,
            vkey = vkey.hexToByteArray(),
            address = address,
            script = script,
            scriptAddress = scriptAddress,
        )

    companion object : UUIDEntityClass<KeyEntity>(KeyTable) {
        fun all(filters: KeyFilters): SizedIterable<KeyEntity> {
            val ops = filters.toOps()
            return if (ops.isEmpty()) KeyEntity.all() else KeyEntity.find(AndOp(ops))
        }

        private fun KeyFilters.toOps(): List<Op<Boolean>> {
            val ops = mutableListOf<Op<Boolean>>()
            olderThan?.let {
                ops += KeyTable.createdAt less it
            }
            newerThan?.let {
                ops += KeyTable.createdAt greater it
            }
            ids?.let {
                ops += KeyTable.id inList it
            }
            address?.let {
                ops += KeyTable.address eq it
            }
            scriptAddress?.let {
                ops += KeyTable.scriptAddress eq it
            }
            return ops
        }
    }
}
