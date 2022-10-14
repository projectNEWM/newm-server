package io.newm.server.features.song.model

import java.util.*

sealed class SongFilter {
    object All : SongFilter()
    data class OwnerId(val value: UUID) : SongFilter()
    data class Genre(val value: String) : SongFilter()
}
