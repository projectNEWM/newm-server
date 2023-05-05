package io.newm.server.features.collaboration.model

import io.ktor.server.application.ApplicationCall
import io.newm.server.ktx.phrase
import io.newm.server.ktx.songIds
import java.util.UUID

data class CollaboratorFilters(
    val songIds: List<UUID>?,
    val phrase: String?
)

val ApplicationCall.collaboratorFilters: CollaboratorFilters
    get() = CollaboratorFilters(songIds, phrase)
