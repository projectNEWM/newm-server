package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OutletStatus(
    @SerialName("status_code")
    val statusCode: OutletStatusCode,
    @SerialName("status_name")
    val statusName: String
)
