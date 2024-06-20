package io.newm.server.features.marketplace.database

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object MarketplacePendingSaleTable : UUIDTable(name = "marketplace_pending_sales") {
    val createdAt: Column<LocalDateTime> = datetime("created_at").defaultExpression(CurrentDateTime)
    val ownerAddress: Column<String> = text("owner_address")
    val bundlePolicyId: Column<String> = text("bundle_policy_id")
    val bundleAssetName: Column<String> = text("bundle_asset_name")
    val bundleAmount: Column<Long> = long("bundle_amount")
    val costPolicyId: Column<String> = text("cost_policy_id")
    val costAssetName: Column<String> = text("cost_asset_name")
    val costAmount: Column<Long> = long("cost_amount")
    val totalBundleQuantity: Column<Long> = long("total_bundle_quantity")
}
