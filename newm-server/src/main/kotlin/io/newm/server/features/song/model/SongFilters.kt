package io.newm.server.features.song.model

import io.ktor.server.application.ApplicationCall
import io.newm.server.ktx.archived
import io.newm.server.ktx.genres
import io.newm.server.ktx.ids
import io.newm.server.ktx.moods
import io.newm.server.ktx.newerThan
import io.newm.server.ktx.olderThan
import io.newm.server.ktx.ownerIds
import io.newm.server.ktx.phrase
import io.newm.server.ktx.sortOrder
import io.newm.server.ktx.sortedBy
import io.newm.server.model.FilterCriteria
import io.newm.server.model.toFilterCriteria
import io.newm.server.model.toStringFilterCriteria
import org.jetbrains.exposed.sql.SortOrder
import java.time.LocalDateTime
import java.util.UUID

data class SongFilters(
    val archived: Boolean?,
    val sortOrder: SortOrder?,
    val sortedBy: String?,
    val olderThan: LocalDateTime?,
    val newerThan: LocalDateTime?,
    val ids: FilterCriteria<UUID>?,
    val ownerIds: FilterCriteria<UUID>?,
    val genres: FilterCriteria<String>?,
    val moods: FilterCriteria<String>?,
    val mintingStatuses: FilterCriteria<MintingStatus>?,
    val nftNames: FilterCriteria<String>?,
    val phrase: String?,
)

val ApplicationCall.mintingStatuses: FilterCriteria<MintingStatus>?
    get() = parameters["mintingStatuses"]?.toFilterCriteria(MintingStatus::valueOf)

val ApplicationCall.nftNames: FilterCriteria<String>?
    get() = parameters["nftNames"]?.toStringFilterCriteria()

val ApplicationCall.songFilters: SongFilters
    get() =
        SongFilters(
            archived,
            sortOrder,
            sortedBy,
            olderThan,
            newerThan,
            ids,
            ownerIds,
            genres,
            moods,
            mintingStatuses,
            nftNames,
            phrase,
        )
