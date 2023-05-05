package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserSubscriptionOverview(
    @SerialName("my_subscription_id")
    val mySubscriptionId: Int,
    @SerialName("expiration_date")
    val expirationDate: String,
    @SerialName("artists_added")
    val artistsAdded: Int,
    @SerialName("tracks_added")
    val tracksAdded: Int,
    @SerialName("albums_added")
    val albumsAdded: Int,
    @SerialName("deactivate_enabled")
    val deactivateEnabled: Boolean,
    @SerialName("reactivate_enabled")
    val reactivateEnabled: Boolean,
    @SerialName("cancel_enabled")
    val cancelEnabled: Boolean,
    @SerialName("status")
    val status: String,
    @SerialName("subscription_details")
    val subscriptionDetails: SubscriptionDetails
)
