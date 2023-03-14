package io.newm.server.features.user.model

import io.ktor.server.application.ApplicationCall
import io.newm.server.ext.genres
import io.newm.server.ext.ids
import io.newm.server.ext.roles
import java.util.UUID

data class UserFilters(
    val ids: List<UUID>?,
    val roles: List<String>?,
    val genres: List<String>?
)

val ApplicationCall.userFilters: UserFilters
    get() = UserFilters(ids, roles, genres)
