package io.newm.server.features.release.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Tracks(
    @SerialName("total") val total: Int,
    @SerialName("items") val items: ArrayList<SpotifyTrackItem> = arrayListOf()
)
