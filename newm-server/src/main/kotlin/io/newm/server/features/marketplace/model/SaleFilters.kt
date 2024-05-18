package io.newm.server.features.marketplace.model

import io.ktor.server.application.ApplicationCall
import io.newm.server.ktx.artistIds
import io.newm.server.ktx.genres
import io.newm.server.ktx.ids
import io.newm.server.ktx.moods
import io.newm.server.ktx.newerThan
import io.newm.server.ktx.olderThan
import io.newm.server.ktx.phrase
import io.newm.server.ktx.songIds
import io.newm.server.ktx.sortOrder
import io.newm.server.model.FilterCriteria
import io.newm.server.model.toFilterCriteria
import org.jetbrains.exposed.sql.SortOrder
import java.time.LocalDateTime
import java.util.UUID

data class SaleFilters(
    val sortOrder: SortOrder?,
    val olderThan: LocalDateTime?,
    val newerThan: LocalDateTime?,
    val ids: FilterCriteria<UUID>?,
    val songIds: FilterCriteria<UUID>?,
    val artistIds: FilterCriteria<UUID>?,
    val statuses: FilterCriteria<SaleStatus>?,
    val genres: FilterCriteria<String>?,
    val moods: FilterCriteria<String>?,
    val phrase: String?
)

val ApplicationCall.saleStatuses: FilterCriteria<SaleStatus>?
    get() = parameters["saleStatuses"]?.toFilterCriteria(SaleStatus::valueOf)

val ApplicationCall.saleFilters: SaleFilters
    get() = SaleFilters(sortOrder, olderThan, newerThan, ids, songIds, artistIds, saleStatuses, genres, moods, phrase)
