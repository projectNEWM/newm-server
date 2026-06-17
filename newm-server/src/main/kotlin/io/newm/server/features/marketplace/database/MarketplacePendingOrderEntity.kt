package io.newm.server.features.marketplace.database

import io.newm.server.typealiases.PendingOrderId
import io.newm.server.typealiases.SaleId
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import java.time.LocalDateTime

class MarketplacePendingOrderEntity(
    id: EntityID<PendingOrderId>
) : UUIDEntity(id) {
    val createdAt: LocalDateTime by MarketplacePendingOrderTable.createdAt
    var saleId: EntityID<SaleId> by MarketplacePendingOrderTable.saleId
    var bundleQuantity: Long by MarketplacePendingOrderTable.bundleQuantity
    var incentiveAmount: Long by MarketplacePendingOrderTable.incentiveAmount
    var serviceFeeAmount: String by MarketplacePendingOrderTable.serviceFeeAmount
    var currencyAmount: String by MarketplacePendingOrderTable.currencyAmount

    companion object : UUIDEntityClass<MarketplacePendingOrderEntity>(MarketplacePendingOrderTable) {
        fun deleteAllExpired(timeToLiveSeconds: Long) {
            MarketplacePendingOrderTable.deleteWhere {
                createdAt lessEq LocalDateTime.now().minusSeconds(timeToLiveSeconds)
            }
        }
    }
}
