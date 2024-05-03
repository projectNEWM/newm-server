package io.newm.server.features.marketplace.database

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.UUID

object MarketplacePendingOrderTable : UUIDTable(name = "marketplace_pending_orders") {
    val createdAt: Column<LocalDateTime> = datetime("created_at").defaultExpression(CurrentDateTime)
    val saleId: Column<EntityID<UUID>> = reference("sale_id", MarketplaceSaleTable, ReferenceOption.RESTRICT, ReferenceOption.RESTRICT)
    val bundleQuantity: Column<Long> = long("bundle_quantity")
    val incentiveAmount: Column<Long> = long("incentive_amount")
}
