package io.newm.server.features.user.model

import io.ktor.server.application.ApplicationCall
import io.newm.server.ktx.genres
import io.newm.server.ktx.ids
import io.newm.server.ktx.newerThan
import io.newm.server.ktx.olderThan
import io.newm.server.ktx.roles
import io.newm.server.ktx.sortOrder
import org.jetbrains.exposed.sql.SortOrder
import java.time.LocalDateTime
import java.util.UUID

data class UserFilters(
    val sortOrder: SortOrder?,
    val olderThan: LocalDateTime?,
    val newerThan: LocalDateTime?,
    val ids: List<UUID>?,
    val roles: List<String>?,
    val genres: List<String>?
)

val ApplicationCall.userFilters: UserFilters
    get() = UserFilters(sortOrder, olderThan, newerThan, ids, roles, genres)
