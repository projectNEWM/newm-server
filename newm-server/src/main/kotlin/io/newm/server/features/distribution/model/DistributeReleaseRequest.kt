package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DistributeReleaseRequest(
    @SerialName("uuid")
    val uuid: String,
    @SerialName("release_id")
    val releaseId: Long? = null,
    @SerialName("outlets_details")
    val outletsDetails: List<OutletsDetail>? = null
)
