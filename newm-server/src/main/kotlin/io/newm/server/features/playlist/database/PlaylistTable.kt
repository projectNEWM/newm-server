package io.newm.server.features.playlist.database

import io.newm.server.features.user.database.UserTable
import io.newm.server.typealiases.UserId
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.javatime.CurrentDateTime
import org.jetbrains.exposed.v1.javatime.datetime
import java.time.LocalDateTime

object PlaylistTable : UUIDTable(name = "playlists") {
    val createdAt: Column<LocalDateTime> = datetime("created_at").defaultExpression(CurrentDateTime)
    val ownerId: Column<EntityID<UserId>> = reference("owner_id", UserTable, onDelete = ReferenceOption.NO_ACTION)
    val name: Column<String> = text("name")
}
