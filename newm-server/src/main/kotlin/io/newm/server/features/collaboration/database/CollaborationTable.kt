package io.newm.server.features.collaboration.database

import io.newm.server.features.collaboration.model.CollaborationStatus
import io.newm.server.features.song.database.SongTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.UUID

object CollaborationTable : UUIDTable(name = "collaborations") {
    val createdAt: Column<LocalDateTime> = datetime("created_at").defaultExpression(CurrentDateTime)
    val songId: Column<EntityID<UUID>> = reference("song_id", SongTable, onDelete = ReferenceOption.NO_ACTION)
    val email: Column<String> = text("email")
    val role: Column<String?> = text("role").nullable()
    val royaltyRate: Column<Float?> = float("royalty_rate").nullable()
    val credited: Column<Boolean> = bool("credited").default(false)
    val featured: Column<Boolean> = bool("featured").default(false)
    val status: Column<CollaborationStatus> =
        enumeration("status", CollaborationStatus::class).default(CollaborationStatus.Editing)
    val distributionArtistId: Column<Long?> = long("distribution_artist_id").nullable()
}
