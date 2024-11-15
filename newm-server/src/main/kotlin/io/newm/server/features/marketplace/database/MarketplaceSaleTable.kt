package io.newm.server.features.marketplace.database

import io.newm.server.features.marketplace.model.SaleStatus
import io.newm.server.features.song.database.SongTable
import io.newm.server.typealiases.SongId
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object MarketplaceSaleTable : UUIDTable(name = "marketplace_sales") {
    val createdAt: Column<LocalDateTime> = datetime("created_at")
    val status: Column<SaleStatus> = enumeration("status", SaleStatus::class)
    val songId: Column<EntityID<SongId>> = reference("song_id", SongTable, ReferenceOption.RESTRICT, ReferenceOption.RESTRICT)
    val ownerAddress: Column<String> = text("owner_address")
    val ownerAddressStakeKey: Column<String?> = text("owner_address_stake_key").nullable()
    val pointerPolicyId: Column<String> = text("pointer_policy_id")
    val pointerAssetName: Column<String> = text("pointer_asset_name")
    val bundlePolicyId: Column<String> = text("bundle_policy_id")
    val bundleAssetName: Column<String> = text("bundle_asset_name")
    val bundleAmount: Column<Long> = long("bundle_amount")
    val costPolicyId: Column<String> = text("cost_policy_id")
    val costAssetName: Column<String> = text("cost_asset_name")
    val costAmount: Column<Long> = long("cost_amount")
    val maxBundleSize: Column<Long> = long("max_bundle_size")
    val totalBundleQuantity: Column<Long> = long("total_bundle_quantity")
    val availableBundleQuantity: Column<Long> = long("available_bundle_quantity")
}
