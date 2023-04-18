package io.newm.server.features.collaboration.model

import io.ktor.server.application.ApplicationCall
import io.newm.server.ktx.emails
import io.newm.server.ktx.ids
import io.newm.server.ktx.newerThan
import io.newm.server.ktx.olderThan
import io.newm.server.ktx.songIds
import java.time.LocalDateTime
import java.util.UUID

data class CollaborationFilters(
    val olderThan: LocalDateTime?,
    val newerThan: LocalDateTime?,
    val ids: List<UUID>?,
    val songIds: List<UUID>?,
    val emails: List<String>?
)

val ApplicationCall.collaborationFilters: CollaborationFilters
    get() = CollaborationFilters(olderThan, newerThan, ids, songIds, emails)
