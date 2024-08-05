package io.newm.server.features.marketplace.database

import io.newm.server.typealiases.BookmarkId
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

object MarketplaceBookmarkTable : IdTable<BookmarkId>(name = "marketplace_bookmarks") {
    override val id: Column<EntityID<BookmarkId>> = text("id").entityId()
    val txId: Column<String> = text("txid")
    val block: Column<Long> = long("block")
    val slot: Column<Long> = long("slot")
}
