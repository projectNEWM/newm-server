package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Subscription(
    @SerialName("subscription_id")
    val subscriptionId: Long,
    @SerialName("partner_reference_id")
    val partnerReferenceId: String
)
