package io.newm.server.features.song.model

import io.ktor.server.application.ApplicationCall
import io.newm.server.ext.genres
import io.newm.server.ext.ids
import io.newm.server.ext.moods
import io.newm.server.ext.newerThan
import io.newm.server.ext.olderThan
import io.newm.server.ext.ownerIds
import io.newm.server.ext.phrase
import java.time.LocalDateTime
import java.util.UUID

data class SongFilters(
    val olderThan: LocalDateTime?,
    val newerThan: LocalDateTime?,
    val ids: List<UUID>?,
    val ownerIds: List<UUID>?,
    val genres: List<String>?,
    val moods: List<String>?,
    val phrase: String?
)

val ApplicationCall.songFilters: SongFilters
    get() = SongFilters(olderThan, newerThan, ids, ownerIds, genres, moods, phrase)
