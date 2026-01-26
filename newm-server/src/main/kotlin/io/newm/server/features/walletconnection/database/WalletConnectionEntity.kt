package io.newm.server.features.walletconnection.database

import io.newm.server.features.walletconnection.model.WalletChain
import io.newm.server.features.walletconnection.model.WalletConnection
import io.newm.server.typealiases.UserId
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import java.time.LocalDateTime
import java.util.UUID

class WalletConnectionEntity(
    id: EntityID<UUID>
) : UUIDEntity(id) {
    val createdAt: LocalDateTime by WalletConnectionTable.createdAt
    var address: String by WalletConnectionTable.address
    var chain: WalletChain by WalletConnectionTable.chain
    var name: String by WalletConnectionTable.name
    var userId: EntityID<UserId>? by WalletConnectionTable.userId

    fun toModel(): WalletConnection =
        WalletConnection(
            id = id.value,
            createdAt = createdAt,
            address = address,
            chain = chain,
            name = name,
            stakeAddress = address // to be removed in future versions
        )

    companion object : UUIDEntityClass<WalletConnectionEntity>(WalletConnectionTable) {
        fun deleteAllExpired(timeToLiveSeconds: Long) {
            WalletConnectionTable.deleteWhere {
                (userId eq null) and (createdAt lessEq LocalDateTime.now().minusSeconds(timeToLiveSeconds))
            }
        }

        fun deleteAllDuplicates(entity: WalletConnectionEntity) {
            WalletConnectionTable.deleteWhere {
                (id neq entity.id) and (userId eq entity.userId) and (address eq entity.address)
            }
        }

        fun getAllByUserId(userId: UserId): SizedIterable<WalletConnectionEntity> =
            find {
                WalletConnectionTable.userId eq userId
            }

        fun getAllByUserIdAndWalletChain(
            userId: UserId,
            chain: WalletChain
        ): SizedIterable<WalletConnectionEntity> =
            find {
                (WalletConnectionTable.userId eq userId) and (WalletConnectionTable.chain eq chain)
            }
    }
}
