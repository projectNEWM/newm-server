package io.newm.server.features.marketplace.model

import io.ktor.server.application.ApplicationCall
import io.newm.server.ktx.artistIds
import io.newm.server.ktx.genres
import io.newm.server.ktx.ids
import io.newm.server.ktx.moods
import io.newm.server.ktx.newerThan
import io.newm.server.ktx.olderThan
import io.newm.server.ktx.phrase
import io.newm.server.ktx.saleStatuses
import io.newm.server.ktx.songIds
import io.newm.server.ktx.sortOrder
import org.jetbrains.exposed.sql.SortOrder
import java.time.LocalDateTime
import java.util.UUID

data class SaleFilters(
    val sortOrder: SortOrder?,
    val olderThan: LocalDateTime?,
    val newerThan: LocalDateTime?,
    val ids: List<UUID>?,
    val songIds: List<UUID>?,
    val artistIds: List<UUID>?,
    val statuses: List<SaleStatus>?,
    val genres: List<String>?,
    val moods: List<String>?,
    val phrase: String?
)

val ApplicationCall.saleFilters: SaleFilters
    get() = SaleFilters(sortOrder, olderThan, newerThan, ids, songIds, artistIds, saleStatuses, genres, moods, phrase)
