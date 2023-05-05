package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionDetails(
    @SerialName("subscription_id")
    val subscriptionId: Int,
    @SerialName("name")
    val name: String,
    @SerialName("sku")
    val sku: String,
    @SerialName("duration")
    val duration: String,
    @SerialName("total_number_of_artist")
    val totalNumberOfArtist: Int,
    @SerialName("total_number_of_tracks")
    val totalNumberOfTracks: Int,
    @SerialName("total_number_of_albums")
    val totalNumberOfAlbums: Int
)
