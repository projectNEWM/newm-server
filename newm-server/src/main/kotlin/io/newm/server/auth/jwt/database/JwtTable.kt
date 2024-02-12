package io.newm.server.auth.jwt.database

import io.newm.server.features.user.database.UserTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.UUID

object JwtTable : UUIDTable(name = "jwts") {
    val userId: Column<EntityID<UUID>> = reference("user_id", UserTable, onDelete = ReferenceOption.NO_ACTION)
    val expiresAt: Column<LocalDateTime> = datetime("expires_at")
}
