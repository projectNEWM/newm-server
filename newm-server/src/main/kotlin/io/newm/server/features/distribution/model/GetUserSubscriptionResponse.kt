package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetUserSubscriptionResponse(
    @SerialName("message")
    val message: String,
    @SerialName("total_records")
    val totalRecords: Long,
    @SerialName("success")
    val success: Boolean,
    @SerialName("data")
    val subscriptions: List<UserSubscriptionOverview>
)
