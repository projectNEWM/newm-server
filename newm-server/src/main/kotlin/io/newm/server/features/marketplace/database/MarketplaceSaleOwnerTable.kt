package io.newm.server.features.marketplace.database

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object MarketplaceSaleOwnerTable : UUIDTable(name = "marketplace_sale_owners") {
    val createdAt: Column<LocalDateTime> = datetime("created_at").defaultExpression(CurrentDateTime)
    val pointerPolicyId: Column<String> = text("pointer_policy_id")
    val pointerAssetName: Column<String> = text("pointer_asset_name")
    val email: Column<String> = text("email")
}
