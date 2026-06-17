package io.newm.server.features.walletconnection.database

import io.newm.server.features.walletconnection.model.ChallengeMethod
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import java.time.LocalDateTime
import java.util.UUID

class WalletConnectionChallengeEntity(
    id: EntityID<UUID>
) : UUIDEntity(id) {
    val createdAt: LocalDateTime by WalletConnectionChallengeTable.createdAt
    var method: ChallengeMethod by WalletConnectionChallengeTable.method
    var stakeAddress: String by WalletConnectionChallengeTable.stakeAddress

    companion object : UUIDEntityClass<WalletConnectionChallengeEntity>(WalletConnectionChallengeTable) {
        fun deleteAllExpired(timeToLiveSeconds: Long) {
            WalletConnectionChallengeTable.deleteWhere {
                createdAt lessEq LocalDateTime.now().minusSeconds(timeToLiveSeconds)
            }
        }
    }
}
