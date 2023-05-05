package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserSubscription(
    @SerialName("subscription_id")
    val subscriptionId: String,
    @SerialName("my_subscription_id")
    val userSubscriptionId: Long
)
