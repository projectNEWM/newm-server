package io.newm.server.auth.jwt.database

import io.newm.server.typealiases.UserId
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.deleteWhere
import java.time.LocalDateTime
import java.util.UUID

class JwtEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    var userId: EntityID<UserId> by JwtTable.userId
    var expiresAt: LocalDateTime by JwtTable.expiresAt

    companion object : UUIDEntityClass<JwtEntity>(JwtTable) {
        fun deleteAllExpired() =
            JwtTable.deleteWhere {
                expiresAt lessEq LocalDateTime.now()
            }
    }
}
