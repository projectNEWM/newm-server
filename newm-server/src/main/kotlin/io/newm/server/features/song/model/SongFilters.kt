package io.newm.server.features.song.model

import io.ktor.server.application.ApplicationCall
import io.newm.server.ext.*
import java.time.LocalDateTime
import java.util.UUID

data class SongFilters(
    val olderThan: LocalDateTime?,
    val newerThan: LocalDateTime?,
    val ids: List<UUID>?,
    val ownerIds: List<UUID>?,
    val genres: List<String>?
)

val ApplicationCall.songFilters: SongFilters
    get() = SongFilters(olderThan, newerThan, ids, ownerIds, genres)
