package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AddUserSubscriptionRequest(
    @SerialName("uuid")
    val uuid: String,
    @SerialName("subscriptions")
    val subscriptions: List<Subscription>
)
