package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DistributeFutureReleaseRequest(
    @SerialName("uuid")
    val uuid: String,
    @SerialName("enable_distribute_to_future_outlets")
    val enableDistributeToFutureOutlets: Boolean
)
