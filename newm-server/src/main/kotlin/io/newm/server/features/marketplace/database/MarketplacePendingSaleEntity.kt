package io.newm.server.features.marketplace.database

import io.newm.server.typealiases.PendingSaleId
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.deleteWhere
import java.time.LocalDateTime

class MarketplacePendingSaleEntity(
    id: EntityID<PendingSaleId>
) : UUIDEntity(id) {
    val createdAt: LocalDateTime by MarketplacePendingSaleTable.createdAt
    var ownerAddress: String by MarketplacePendingSaleTable.ownerAddress
    var bundlePolicyId: String by MarketplacePendingSaleTable.bundlePolicyId
    var bundleAssetName: String by MarketplacePendingSaleTable.bundleAssetName
    var bundleAmount: Long by MarketplacePendingSaleTable.bundleAmount
    var costPolicyId: String by MarketplacePendingSaleTable.costPolicyId
    var costAssetName: String by MarketplacePendingSaleTable.costAssetName
    var costAmount: Long by MarketplacePendingSaleTable.costAmount
    var totalBundleQuantity: Long by MarketplacePendingSaleTable.totalBundleQuantity

    companion object : UUIDEntityClass<MarketplacePendingSaleEntity>(MarketplacePendingSaleTable) {
        fun deleteAllExpired(timeToLiveSeconds: Long) {
            MarketplacePendingSaleTable.deleteWhere {
                createdAt lessEq LocalDateTime.now().minusSeconds(timeToLiveSeconds)
            }
        }
    }
}
