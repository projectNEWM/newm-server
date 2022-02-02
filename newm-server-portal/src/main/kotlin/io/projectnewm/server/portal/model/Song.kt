package io.projectnewm.server.portal.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Song(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("description") val description: String,
    @SerialName("genres") val genres: List<String>,
    @SerialName("releaseDate") val releaseDate: LocalDate,
    @SerialName("albumImageUrl") val albumImageUrl: String,
    @SerialName("extraInfo") val extraInfo: String,
    @SerialName("contributors") val contributors: List<Contributor>
)
