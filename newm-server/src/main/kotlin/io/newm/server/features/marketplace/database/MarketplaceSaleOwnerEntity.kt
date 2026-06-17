package io.newm.server.features.marketplace.database

import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
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
