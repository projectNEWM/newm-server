package io.newm.server.features.marketplace.database

import io.newm.server.typealiases.BookmarkId
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.Column

object MarketplaceBookmarkTable : IdTable<BookmarkId>(name = "marketplace_bookmarks") {
    override val id: Column<EntityID<BookmarkId>> = text("id").entityId()
    val txId: Column<String> = text("txid")
    val block: Column<Long> = long("block")
    val slot: Column<Long> = long("slot")
}
