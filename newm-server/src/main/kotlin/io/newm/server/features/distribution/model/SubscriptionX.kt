package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionX(
    @SerialName("expiration_date")
    val expirationDate: String,
    @SerialName("subscription_id")
    val subscriptionId: Int,
    @SerialName("subscription_name")
    val subscriptionName: String
)
