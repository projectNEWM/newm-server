package io.newm.server.features.marketplace.database

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.UUID

object MarketplacePurchaseTable : UUIDTable(name = "marketplace_purchases") {
    val createdAt: Column<LocalDateTime> = datetime("created_at")
    val saleId: Column<EntityID<UUID>> = reference("sale_id", MarketplaceSaleTable, ReferenceOption.RESTRICT, ReferenceOption.RESTRICT)
    val bundleQuantity: Column<Long> = long("bundle_quantity")
}
