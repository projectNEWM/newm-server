package io.newm.server.features.song.database

import io.newm.server.typealiases.SongId
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object SongSmartLinkTable : UUIDTable(name = "song_smart_links") {
    val createdAt: Column<LocalDateTime> = datetime("created_at").defaultExpression(CurrentDateTime)
    val songId: Column<EntityID<SongId>> = reference("song_id", SongTable, onDelete = ReferenceOption.NO_ACTION).index()
    val storeName: Column<String> = text("store_name")
    val url: Column<String> = text("url")
}
