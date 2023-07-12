package io.newm.server.features.collaboration.model

import io.ktor.server.application.ApplicationCall
import io.newm.server.ktx.phrase
import io.newm.server.ktx.songIds
import io.newm.server.ktx.sortOrder
import org.jetbrains.exposed.sql.SortOrder
import java.util.UUID

data class CollaboratorFilters(
    val sortOrder: SortOrder?,
    val excludeMe: Boolean?,
    val songIds: List<UUID>?,
    val phrase: String?
)

val ApplicationCall.excludeMe: Boolean?
    get() = parameters["excludeMe"]?.toBoolean()

val ApplicationCall.collaboratorFilters: CollaboratorFilters
    get() = CollaboratorFilters(sortOrder, excludeMe, songIds, phrase)
