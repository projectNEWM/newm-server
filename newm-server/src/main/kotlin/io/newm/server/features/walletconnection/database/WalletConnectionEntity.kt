package io.newm.server.features.walletconnection.database

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import java.time.LocalDateTime
import java.util.UUID

class WalletConnectionEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    val createdAt: LocalDateTime by WalletConnectionTable.createdAt
    var stakeAddress: String by WalletConnectionTable.stakeAddress
    var userId: EntityID<UUID>? by WalletConnectionTable.userId

    companion object : UUIDEntityClass<WalletConnectionEntity>(WalletConnectionTable) {
        fun deleteAllExpired(timeToLiveSeconds: Long) {
            WalletConnectionTable.deleteWhere {
                (userId eq null) and (createdAt lessEq LocalDateTime.now().minusSeconds(timeToLiveSeconds))
            }
        }
    }
}
