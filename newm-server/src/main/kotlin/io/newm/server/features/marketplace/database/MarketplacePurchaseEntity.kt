package io.newm.server.features.marketplace.database

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.time.LocalDateTime
import java.util.UUID

class MarketplacePurchaseEntity(
    id: EntityID<UUID>
) : UUIDEntity(id) {
    var createdAt: LocalDateTime by MarketplacePurchaseTable.createdAt
    var saleId: EntityID<UUID> by MarketplacePurchaseTable.saleId
    var bundleQuantity: Long by MarketplacePurchaseTable.bundleQuantity

    companion object : UUIDEntityClass<MarketplacePurchaseEntity>(MarketplacePurchaseTable)
}
