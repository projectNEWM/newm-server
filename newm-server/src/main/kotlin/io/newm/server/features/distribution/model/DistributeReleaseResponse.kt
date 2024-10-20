package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DistributeReleaseResponse(
    @SerialName("message")
    val message: String,
    @SerialName("success")
    val success: Boolean,
    @SerialName("data")
    val releaseData: ReleaseData? = null,
)
