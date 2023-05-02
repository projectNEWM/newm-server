package io.newm.server.features.collaboration.model

import io.ktor.server.application.ApplicationCall
import io.newm.server.ktx.ids
import io.newm.server.ktx.newerThan
import io.newm.server.ktx.olderThan
import io.newm.server.ktx.songIds
import io.newm.shared.ktx.splitAndTrim
import java.time.LocalDateTime
import java.util.UUID

data class CollaborationFilters(
    val inbound: Boolean?,
    val olderThan: LocalDateTime?,
    val newerThan: LocalDateTime?,
    val ids: List<UUID>?,
    val songIds: List<UUID>?,
    val statuses: List<CollaborationStatus>?
)

val ApplicationCall.inbound: Boolean?
    get() = parameters["inbound"]?.toBoolean()

val ApplicationCall.statuses: List<CollaborationStatus>?
    get() = parameters["statuses"]?.splitAndTrim()?.map(CollaborationStatus::valueOf)

val ApplicationCall.collaborationFilters: CollaborationFilters
    get() = CollaborationFilters(inbound, olderThan, newerThan, ids, songIds, statuses)
