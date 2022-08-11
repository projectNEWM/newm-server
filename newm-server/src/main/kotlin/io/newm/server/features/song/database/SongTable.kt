package io.newm.server.features.song.database

import io.newm.server.features.user.database.UserTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object SongTable : UUIDTable(name = "songs") {
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val ownerId = reference("owner_id", UserTable, onDelete = ReferenceOption.CASCADE)
    val title = text("title")
    val genre = text("genre").nullable()
    val coverArtUrl = text("cover_art_url").nullable()
    val description = text("description").nullable()
    val credits = text("credits").nullable()
    val audioUrl = text("audio_url").nullable()
    val nftPolicyId = text("nft_policy_id").nullable()
    val nftName = text("nft_name").nullable()
}
