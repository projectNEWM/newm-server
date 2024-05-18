package io.newm.server.features.collaboration.model

import io.ktor.server.application.ApplicationCall
import io.newm.server.ktx.emails
import io.newm.server.ktx.ids
import io.newm.server.ktx.newerThan
import io.newm.server.ktx.olderThan
import io.newm.server.ktx.songIds
import io.newm.server.ktx.sortOrder
import io.newm.server.model.FilterCriteria
import io.newm.server.model.toFilterCriteria
import org.jetbrains.exposed.sql.SortOrder
import java.time.LocalDateTime
import java.util.UUID

data class CollaborationFilters(
    val sortOrder: SortOrder? = null,
    val inbound: Boolean? = null,
    val olderThan: LocalDateTime? = null,
    val newerThan: LocalDateTime? = null,
    val ids: FilterCriteria<UUID>? = null,
    val songIds: FilterCriteria<UUID>? = null,
    val emails: FilterCriteria<String>? = null,
    val statuses: FilterCriteria<CollaborationStatus>? = null
)

val ApplicationCall.inbound: Boolean?
    get() = parameters["inbound"]?.toBoolean()

val ApplicationCall.statuses: FilterCriteria<CollaborationStatus>?
    get() = parameters["statuses"]?.toFilterCriteria(CollaborationStatus::valueOf)

val ApplicationCall.collaborationFilters: CollaborationFilters
    get() = CollaborationFilters(sortOrder, inbound, olderThan, newerThan, ids, songIds, emails, statuses)
