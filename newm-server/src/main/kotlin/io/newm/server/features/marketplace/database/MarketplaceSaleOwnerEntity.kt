package io.newm.server.features.marketplace.database

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.and
import java.time.LocalDateTime
import java.util.UUID

class MarketplaceSaleOwnerEntity(
    id: EntityID<UUID>
) : UUIDEntity(id) {
    val createdAt: LocalDateTime by MarketplaceSaleOwnerTable.createdAt
    var pointerPolicyId: String by MarketplaceSaleOwnerTable.pointerPolicyId
    var pointerAssetName: String by MarketplaceSaleOwnerTable.pointerAssetName
    var email: String by MarketplaceSaleOwnerTable.email

    companion object : UUIDEntityClass<MarketplaceSaleOwnerEntity>(MarketplaceSaleOwnerTable) {
        fun getByPointer(
            policyId: String,
            assetName: String
        ): MarketplaceSaleOwnerEntity? =
            MarketplaceSaleOwnerEntity
                .find {
                    (MarketplaceSaleOwnerTable.pointerPolicyId eq policyId) and
                        (MarketplaceSaleOwnerTable.pointerAssetName eq assetName)
                }.firstOrNull()
    }
}
