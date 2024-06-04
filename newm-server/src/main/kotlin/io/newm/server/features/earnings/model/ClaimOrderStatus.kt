package io.newm.server.features.earnings.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ClaimOrderStatus {
    @SerialName("Pending")
    Pending,

    @SerialName("Processing")
    Processing,

    @SerialName("Completed")
    Completed,

    @SerialName("Timeout")
    Timeout,

    @SerialName("Blocked")
    Blocked,

    @SerialName("Failed")
    Failed,
}
