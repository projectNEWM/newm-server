package io.newm.server.features.marketplace.model

import io.ktor.server.application.ApplicationCall
import io.newm.server.ktx.genres
import io.newm.server.ktx.ids
import io.newm.server.ktx.newerThan
import io.newm.server.ktx.olderThan
import io.newm.server.ktx.sortOrder
import io.newm.server.model.FilterCriteria
import io.newm.server.typealiases.UserId
import org.jetbrains.exposed.sql.SortOrder
import java.time.LocalDateTime

data class ArtistFilters(
    val sortOrder: SortOrder?,
    val olderThan: LocalDateTime?,
    val newerThan: LocalDateTime?,
    val ids: FilterCriteria<UserId>?,
    val genres: FilterCriteria<String>?
)

val ApplicationCall.artistFilters: ArtistFilters
    get() = ArtistFilters(sortOrder, olderThan, newerThan, ids, genres)
