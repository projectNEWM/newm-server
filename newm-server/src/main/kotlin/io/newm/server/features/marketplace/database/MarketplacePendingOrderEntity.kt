package io.newm.server.features.marketplace.database

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.deleteWhere
import java.time.LocalDateTime
import java.util.UUID

class MarketplacePendingOrderEntity(
    id: EntityID<UUID>
) : UUIDEntity(id) {
    val createdAt: LocalDateTime by MarketplacePendingOrderTable.createdAt
    var saleId: EntityID<UUID> by MarketplacePendingOrderTable.saleId
    var bundleQuantity: Long by MarketplacePendingOrderTable.bundleQuantity
    var incentiveAmount: Long by MarketplacePendingOrderTable.incentiveAmount

    companion object : UUIDEntityClass<MarketplacePendingOrderEntity>(MarketplacePendingOrderTable) {
        fun deleteAllExpired(timeToLiveSeconds: Long) {
            MarketplacePendingOrderTable.deleteWhere {
                createdAt lessEq LocalDateTime.now().minusSeconds(timeToLiveSeconds)
            }
        }
    }
}
