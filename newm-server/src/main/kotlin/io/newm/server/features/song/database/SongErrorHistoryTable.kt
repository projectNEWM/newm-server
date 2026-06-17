package io.newm.server.features.song.database

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.javatime.CurrentDateTime
import org.jetbrains.exposed.v1.javatime.datetime
import java.time.LocalDateTime
import java.util.UUID

object SongErrorHistoryTable : UUIDTable(name = "song_error_history") {
    val createdAt: Column<LocalDateTime> = datetime("created_at").defaultExpression(CurrentDateTime)
    val songId: Column<EntityID<UUID>> =
        reference("song_id", SongTable, onUpdate = ReferenceOption.NO_ACTION, onDelete = ReferenceOption.NO_ACTION)
    val errorMessage: Column<String> = text("error_message")
}
