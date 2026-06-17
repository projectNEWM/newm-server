package io.newm.server.features.marketplace.database

import io.newm.server.typealiases.SaleId
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.javatime.datetime
import java.time.LocalDateTime

object MarketplacePurchaseTable : UUIDTable(name = "marketplace_purchases") {
    val createdAt: Column<LocalDateTime> = datetime("created_at")
    val saleId: Column<EntityID<SaleId>> = reference("sale_id", MarketplaceSaleTable, ReferenceOption.RESTRICT, ReferenceOption.RESTRICT)
    val bundleQuantity: Column<Long> = long("bundle_quantity")
}
