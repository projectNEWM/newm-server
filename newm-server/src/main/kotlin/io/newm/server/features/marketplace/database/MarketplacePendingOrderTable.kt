package io.newm.server.features.marketplace.database

import io.newm.server.typealiases.SaleId
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.javatime.CurrentDateTime
import org.jetbrains.exposed.v1.javatime.datetime
import java.time.LocalDateTime

object MarketplacePendingOrderTable : UUIDTable(name = "marketplace_pending_orders") {
    val createdAt: Column<LocalDateTime> = datetime("created_at").defaultExpression(CurrentDateTime)
    val saleId: Column<EntityID<SaleId>> = reference("sale_id", MarketplaceSaleTable, ReferenceOption.RESTRICT, ReferenceOption.RESTRICT)
    val bundleQuantity: Column<Long> = long("bundle_quantity")
    val incentiveAmount: Column<Long> = long("incentive_amount")
    val serviceFeeAmount: Column<String> = text("service_fee_amount")
    val currencyAmount: Column<String> = text("currency_amount")
}
