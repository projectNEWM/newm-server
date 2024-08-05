package io.newm.server.features.marketplace.database

import io.newm.server.typealiases.PurchaseId
import io.newm.server.typealiases.SaleId
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.time.LocalDateTime

class MarketplacePurchaseEntity(
    id: EntityID<PurchaseId>
) : UUIDEntity(id) {
    var createdAt: LocalDateTime by MarketplacePurchaseTable.createdAt
    var saleId: EntityID<SaleId> by MarketplacePurchaseTable.saleId
    var bundleQuantity: Long by MarketplacePurchaseTable.bundleQuantity

    companion object : UUIDEntityClass<MarketplacePurchaseEntity>(MarketplacePurchaseTable)
}
