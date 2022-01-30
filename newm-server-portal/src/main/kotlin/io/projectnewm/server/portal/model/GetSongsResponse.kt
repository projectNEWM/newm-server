package io.projectnewm.server.portal.model

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class GetSongsResponse(
    @SerialName("version") val version: Int,
    @SerialName("time") val time: LocalDateTime,
    @SerialName("songs") val songs: List<Song>,
)