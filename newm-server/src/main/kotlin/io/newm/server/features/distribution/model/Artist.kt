package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class Artist(
    @SerialName("id")
    @JsonNames("id", "artist_id")
    val id: Long,
    @SerialName("name")
    @JsonNames("name", "artist_name")
    val name: String
)
