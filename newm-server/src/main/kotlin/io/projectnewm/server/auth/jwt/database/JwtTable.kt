package io.projectnewm.server.auth.jwt.database

import io.projectnewm.server.features.user.database.UserTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.datetime

object JwtTable : UUIDTable(name = "jwts") {
    val userId = reference("user_id", UserTable, onDelete = ReferenceOption.CASCADE)
    val expiresAt = datetime("expires_at")
}
