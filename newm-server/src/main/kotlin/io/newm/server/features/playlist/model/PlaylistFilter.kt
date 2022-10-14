package io.newm.server.features.playlist.model

import java.util.*

sealed interface PlaylistFilter {
    object All : PlaylistFilter
    data class OwnerId(val value: UUID) : PlaylistFilter
}
