package io.newm.server.features.marketplace.database

import io.newm.chain.grpc.MonitorAddressResponse
import io.newm.server.typealiases.BookmarkId
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class MarketplaceBookmarkEntity(
    id: EntityID<BookmarkId>
) : Entity<BookmarkId>(id) {
    var txId: String by MarketplaceBookmarkTable.txId
    var block: Long by MarketplaceBookmarkTable.block
    var slot: Long by MarketplaceBookmarkTable.slot

    companion object : EntityClass<BookmarkId, MarketplaceBookmarkEntity>(MarketplaceBookmarkTable) {
        fun update(
            id: BookmarkId,
            response: MonitorAddressResponse
        ) {
            val doUpdate = { entity: MarketplaceBookmarkEntity ->
                entity.txId = response.txId
                entity.block = response.block
                entity.slot = response.slot
            }
            findById(id)?.let(doUpdate) ?: new(id, doUpdate)
        }
    }
}
