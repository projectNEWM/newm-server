package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PayoutStatus(
    @SerialName("status_code")
    val statusCode: PayoutStatusCode,
    @SerialName("status_name")
    val statusName: String
)
