package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Genre(
    @SerialName("genre_id")
    val genreId: Long,
    @SerialName("name")
    val name: String
)
