package io.projectnewm.server.features.playlist.database

import io.projectnewm.server.features.user.database.UserTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object PlaylistTable : UUIDTable(name = "playlists") {
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val ownerId = reference("owner_id", UserTable, onDelete = ReferenceOption.CASCADE)
    val name = text("name")
}
