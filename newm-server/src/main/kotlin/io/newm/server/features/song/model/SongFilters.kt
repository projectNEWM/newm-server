package io.newm.server.features.song.model

import io.ktor.server.application.ApplicationCall
import io.newm.server.ktx.genres
import io.newm.server.ktx.ids
import io.newm.server.ktx.moods
import io.newm.server.ktx.newerThan
import io.newm.server.ktx.olderThan
import io.newm.server.ktx.ownerIds
import io.newm.server.ktx.phrase
import io.newm.shared.ktx.splitAndTrim
import java.time.LocalDateTime
import java.util.UUID

data class SongFilters(
    val olderThan: LocalDateTime?,
    val newerThan: LocalDateTime?,
    val ids: List<UUID>?,
    val ownerIds: List<UUID>?,
    val genres: List<String>?,
    val moods: List<String>?,
    val mintingStatuses: List<MintingStatus>?,
    val phrase: String?
)

val ApplicationCall.mintingStatuses: List<MintingStatus>?
    get() = parameters["mintingStatuses"]?.splitAndTrim()?.map(MintingStatus::valueOf)

val ApplicationCall.songFilters: SongFilters
    get() = SongFilters(olderThan, newerThan, ids, ownerIds, genres, moods, mintingStatuses, phrase)
