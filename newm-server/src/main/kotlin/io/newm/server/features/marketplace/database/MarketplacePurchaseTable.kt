package io.newm.server.features.marketplace.database

import io.newm.server.typealiases.SaleId
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object MarketplacePurchaseTable : UUIDTable(name = "marketplace_purchases") {
    val createdAt: Column<LocalDateTime> = datetime("created_at")
    val saleId: Column<EntityID<SaleId>> = reference("sale_id", MarketplaceSaleTable, ReferenceOption.RESTRICT, ReferenceOption.RESTRICT)
    val bundleQuantity: Column<Long> = long("bundle_quantity")
}
