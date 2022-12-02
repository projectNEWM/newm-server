package io.newm.server.features.song.database

import io.newm.server.features.song.model.MarketplaceStatus
import io.newm.server.features.song.model.MintingStatus
import io.newm.server.features.user.database.UserTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.UUID

object SongTable : UUIDTable(name = "songs") {
    val createdAt: Column<LocalDateTime> = datetime("created_at").defaultExpression(CurrentDateTime)
    val ownerId: Column<EntityID<UUID>> = reference("owner_id", UserTable, onDelete = ReferenceOption.CASCADE)
    val title: Column<String> = text("title")
    val genre: Column<String> = text("genre")
    val coverArtUrl: Column<String?> = text("cover_art_url").nullable()
    val description: Column<String?> = text("description").nullable()
    val credits: Column<String?> = text("credits").nullable()
    val duration: Column<Int?> = integer("duration").nullable()
    val streamUrl: Column<String?> = text("stream_url").nullable()
    val nftPolicyId: Column<String?> = text("nft_policy_id").nullable()
    val nftName: Column<String?> = text("nft_name").nullable()
    val mintingStatus: Column<MintingStatus?> = enumeration("minting_status", MintingStatus::class).nullable()
    val marketplaceStatus: Column<MarketplaceStatus?> =
        enumeration("marketplace_status", MarketplaceStatus::class).nullable()
}
