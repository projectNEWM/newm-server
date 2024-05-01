package io.newm.server.features.playlist.database

import io.newm.server.features.user.database.UserTable
import io.newm.server.typealiases.UserId
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object PlaylistTable : UUIDTable(name = "playlists") {
    val createdAt: Column<LocalDateTime> = datetime("created_at").defaultExpression(CurrentDateTime)
    val ownerId: Column<EntityID<UserId>> = reference("owner_id", UserTable, onDelete = ReferenceOption.NO_ACTION)
    val name: Column<String> = text("name")
}
