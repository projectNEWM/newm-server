package io.newm.server.features.marketplace.database

import io.newm.server.typealiases.SaleId
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object MarketplacePendingOrderTable : UUIDTable(name = "marketplace_pending_orders") {
    val createdAt: Column<LocalDateTime> = datetime("created_at").defaultExpression(CurrentDateTime)
    val saleId: Column<EntityID<SaleId>> = reference("sale_id", MarketplaceSaleTable, ReferenceOption.RESTRICT, ReferenceOption.RESTRICT)
    val bundleQuantity: Column<Long> = long("bundle_quantity")
    val incentiveAmount: Column<Long> = long("incentive_amount")
    val currencyAmount: Column<String> = text("currency_amount")
}
