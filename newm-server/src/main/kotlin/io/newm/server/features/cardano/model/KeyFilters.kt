package io.newm.server.features.cardano.model

import java.time.LocalDateTime
import java.util.UUID

data class KeyFilters(
    val olderThan: LocalDateTime?,
    val newerThan: LocalDateTime?,
    val ids: List<UUID>?,
    val address: String?,
    val scriptAddress: String?,
)

// val ApplicationCall.keyFilters: KeyFilters
//    get() = KeyFilters(olderThan, newerThan, ids, address, scriptAddress)
