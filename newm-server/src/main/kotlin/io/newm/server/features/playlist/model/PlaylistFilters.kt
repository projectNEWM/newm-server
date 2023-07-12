package io.newm.server.features.playlist.model

import io.ktor.server.application.ApplicationCall
import io.newm.server.ktx.ids
import io.newm.server.ktx.newerThan
import io.newm.server.ktx.olderThan
import io.newm.server.ktx.ownerIds
import io.newm.server.ktx.sortOrder
import org.jetbrains.exposed.sql.SortOrder
import java.time.LocalDateTime
import java.util.UUID

data class PlaylistFilters(
    val sortOrder: SortOrder?,
    val olderThan: LocalDateTime?,
    val newerThan: LocalDateTime?,
    val ids: List<UUID>?,
    val ownerIds: List<UUID>?
)

val ApplicationCall.playlistFilters: PlaylistFilters
    get() = PlaylistFilters(sortOrder, olderThan, newerThan, ids, ownerIds)
