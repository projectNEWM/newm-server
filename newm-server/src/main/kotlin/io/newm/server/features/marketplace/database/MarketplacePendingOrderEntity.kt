package io.newm.server.features.marketplace.database

import io.newm.server.typealiases.PendingOrderId
import io.newm.server.typealiases.SaleId
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.deleteWhere
import java.time.LocalDateTime

class MarketplacePendingOrderEntity(
    id: EntityID<PendingOrderId>
) : UUIDEntity(id) {
    val createdAt: LocalDateTime by MarketplacePendingOrderTable.createdAt
    var saleId: EntityID<SaleId> by MarketplacePendingOrderTable.saleId
    var bundleQuantity: Long by MarketplacePendingOrderTable.bundleQuantity
    var incentiveAmount: Long by MarketplacePendingOrderTable.incentiveAmount
    var currencyAmount: String by MarketplacePendingOrderTable.currencyAmount

    companion object : UUIDEntityClass<MarketplacePendingOrderEntity>(MarketplacePendingOrderTable) {
        fun deleteAllExpired(timeToLiveSeconds: Long) {
            MarketplacePendingOrderTable.deleteWhere {
                createdAt lessEq LocalDateTime.now().minusSeconds(timeToLiveSeconds)
            }
        }
    }
}
