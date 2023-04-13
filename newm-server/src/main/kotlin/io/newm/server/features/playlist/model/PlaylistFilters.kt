package io.newm.server.features.playlist.model

import io.ktor.server.application.ApplicationCall
import io.newm.server.ktx.ids
import io.newm.server.ktx.newerThan
import io.newm.server.ktx.olderThan
import io.newm.server.ktx.ownerIds
import java.time.LocalDateTime
import java.util.UUID

data class PlaylistFilters(
    val olderThan: LocalDateTime?,
    val newerThan: LocalDateTime?,
    val ids: List<UUID>?,
    val ownerIds: List<UUID>?
)

val ApplicationCall.playlistFilters: PlaylistFilters
    get() = PlaylistFilters(olderThan, newerThan, ids, ownerIds)
